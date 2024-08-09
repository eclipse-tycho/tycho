/*******************************************************************************
 * Copyright (c) 2021, 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation based on 
 *    		org.eclipse.tycho.p2.util.resolution.AbstractSlicerResolutionStrategy
 *    		org.eclipse.tycho.p2.util.resolution.ProjectorResolutionStrategy
 *******************************************************************************/
package org.eclipse.tycho.build;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.graph.DefaultGraphBuilder;
import org.apache.maven.graph.DefaultProjectDependencyGraph;
import org.apache.maven.graph.GraphBuilder;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.building.DefaultModelProblem;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.Result;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.p2maven.MavenProjectDependencyProcessor;
import org.eclipse.tycho.p2maven.MavenProjectDependencyProcessor.ProjectDependencies;
import org.eclipse.tycho.p2maven.MavenProjectDependencyProcessor.ProjectDependencyClosure;
import org.eclipse.tycho.pomless.AbstractTychoMapping;
import org.sonatype.maven.polyglot.mapping.Mapping;

@Named(GraphBuilder.HINT)
@Singleton
public class TychoGraphBuilder extends DefaultGraphBuilder {

	private static final boolean DEBUG = Boolean.getBoolean("tycho.graphbuilder.debug");
	@Inject
	private Logger log;

	@Inject
	private Map<String, Mapping> polyglotMappings;

	@Inject
	private MavenProjectDependencyProcessor dependencyProcessor;

	@Override
	public Result<ProjectDependencyGraph> build(MavenSession session) {
		Objects.requireNonNull(session);
		// Tell the polyglot mappings that we are in extension mode
		for (Mapping mapping : polyglotMappings.values()) {
			if (mapping instanceof AbstractTychoMapping tychoMapping) {
				tychoMapping.setExtensionMode(true);
				tychoMapping.setMultiModuleProjectDirectory(session.getRequest().getMultiModuleProjectDirectory());
				Properties properties = session.getRequest().getSystemProperties();
				if (properties.getProperty(TychoCiFriendlyVersions.PROPERTY_BUILDQUALIFIER_FORMAT) != null
						|| properties.getProperty(TychoCiFriendlyVersions.PROPERTY_FORCE_QUALIFIER) != null
						|| properties.getProperty(TychoCiFriendlyVersions.BUILD_QUALIFIER) != null) {
					tychoMapping.setSnapshotProperty(TychoCiFriendlyVersions.BUILD_QUALIFIER);
				}
			}
		}
		MavenExecutionRequest request = session.getRequest();
		ProjectDependencyGraph dependencyGraph = session.getProjectDependencyGraph();
		Result<ProjectDependencyGraph> graphResult = super.build(session);
		if (dependencyGraph != null || graphResult.hasErrors()) {
			// on second pass nothing to do for tycho, or already error ...
			return graphResult;
		}
		session.getUserProperties().put(TychoConstants.SESSION_PROPERTY_TYCHO_MODE, "extension");
		if (TychoConstants.USE_SMART_BUILDER && session.getRequest().getDegreeOfConcurrency() > 1) {
			request.setBuilderId("smart");
			session.getUserProperties().put(TychoConstants.SESSION_PROPERTY_TYCHO_BUILDER, "smart");
		}
		String makeBehavior = request.getMakeBehavior();
		if (DEBUG) {
			log.info("TychoGraphBuilder:");
			log.info("  - SelectedProjects: " + request.getSelectedProjects());
			log.info("  - ExcludedProjects: " + request.getExcludedProjects());
			log.info("  - MakeBehavior:     " + makeBehavior);
		}
		// upstream is the -am / --also-make option of maven described as:
		// When you specify a project with the -am option, Maven will build all of the
		// projects that the specified project depends upon (either directly or
		// indirectly). Maven will examine the list of projects and walk down the
		// dependency tree, finding all of the projects that it needs to build.
		// If you are working on the multi-module project with the build order shown in
		// Order of Project Builds in Maven Reactor and you were only interested in
		// working on the sample-services project, you would run mvn -pl simple-services
		// -am to build only those projects
		// $ mvn --projects sample-services --also-make install
		boolean makeUpstream = MavenExecutionRequest.REACTOR_MAKE_UPSTREAM.equals(makeBehavior)
				|| MavenExecutionRequest.REACTOR_MAKE_BOTH.equals(makeBehavior);

		// downstream is the -amd / -also-make-dependents option of maven that is
		// described as:
		// While the -am command makes all of the projects required by a particular
		// project in a multi-module build, the -amd or --also-make-dependents option
		// configures Maven to build a project and any project that depends on that
		// project. When using --also-make-dependents, Maven will examine all of the
		// projects in our reactor to find projects that depend on a particular project.
		// It will automatically build those projects and nothing else.
		// If you are working on the multi-module project with the build order shown in
		// Order of Project Builds in Maven Reactor and you wanted to make sure that
		// your changes to sample-services did not introduce any errors into the
		// projects that directly or indirectly depend on sample-services, you would run
		// the following command:
		// $ mvn --projects sample-services --also-make-dependents install
		boolean makeDownstream = MavenExecutionRequest.REACTOR_MAKE_DOWNSTREAM.equals(makeBehavior)
				|| MavenExecutionRequest.REACTOR_MAKE_BOTH.equals(makeBehavior);
		if (!makeDownstream && !makeUpstream) {
			// just like default...
			return graphResult;
		}
		ProjectDependencyGraph graph = graphResult.get();
		List<MavenProject> projects = graph.getAllProjects();
		Map<String, MavenProject> projectIdMap = projects.stream()
				.collect(Collectors.toMap(p -> getProjectKey(p), Function.identity()));
		int degreeOfConcurrency = request.getDegreeOfConcurrency();
		Optional<ExecutorService> executor;
		if (degreeOfConcurrency > 1) {
			executor = Optional.of(new ForkJoinPool(degreeOfConcurrency));
		} else {
			executor = Optional.empty();
		}
		Set<MavenProject> selectedProjects = ConcurrentHashMap.newKeySet();
		try {
			ProjectDependencyClosure dependencyClosure;
			try {
				dependencyClosure = dependencyProcessor.computeProjectDependencyClosure(projects, session);
			} catch (CoreException e) {
				log.error("Cannot resolve projects", e);
				return Result.error(graph, toProblems(e.getStatus(), new ArrayList<>()));
			}

			if (DEBUG) {
				for (MavenProject project : projects) {
					ProjectDependencies depends = dependencyClosure.getProjectDependecies(project);
					// we fetch all dependencies here without filtering, because the goal is to find
					// as many projects that are maybe required
					Collection<IInstallableUnit> dependencies = depends.getDependencies(List.of());
					if (dependencies.isEmpty()) {
						continue;
					}
					log.info("[[ project " + project.getName() + " depends on: ]]");
					for (IInstallableUnit dependency : dependencies) {
						Optional<MavenProject> mavenProject = dependencyClosure.getProject(dependency);
						if (mavenProject.isEmpty()) {
							log.info(" IU: " + dependency);
						} else {
							log.info(" IU: " + dependency + " [of project " + mavenProject.get().getName() + "]");
						}
					}
				}
			}
			Queue<ProjectRequest> queue = new ConcurrentLinkedQueue<>(graph.getSortedProjects().stream()
					.map(p -> new ProjectRequest(p, makeDownstream, makeUpstream, null)).toList());
			if (DEBUG) {
				log.info("Computing additional " + makeBehavior
					+ " dependencies based on initial project set of " + queue.stream().map(r -> r.mavenProject)
							.map(MavenProject::getName).collect(Collectors.joining(", ")));
			}
			while (!queue.isEmpty()) {
				ProjectRequest projectRequest = queue.poll();
				if (selectedProjects.add(projectRequest.mavenProject)) {
					if (projectRequest.addDependencies) {
						// we fetch all dependencies here without filtering for the context, because the
						// goal is to find as many projects that are might be required
						dependencyClosure.getDependencyProjects(projectRequest.mavenProject, List.of())
								.forEach(project -> {
							if (DEBUG) {
								log.info(" + add dependency project '" + project.getId() + "' of project '"
									+ projectRequest.mavenProject.getId() + "'");
							}
							// we also need to add the dependencies of the dependency project
							queue.add(new ProjectRequest(project, false, true, projectRequest));
						});
						// special case: a (transitive) Tycho project might have declared a dependency
						// to another project in the reactor but this can not be discovered by maven
						// before we add it here...
						List<Dependency> dependencies = projectRequest.mavenProject.getDependencies();
						for (Dependency dependency : dependencies) {
							MavenProject reactorMavenProjectDependency = projectIdMap.get(getProjectKey(dependency));
							if (reactorMavenProjectDependency != null) {
								if (DEBUG) {
									log.info(" + add (maven) dependency project '"
											+ reactorMavenProjectDependency.getId() + "' of project '"
											+ projectRequest.mavenProject.getId() + "'");
								}
								queue.add(
										new ProjectRequest(reactorMavenProjectDependency, false, true, projectRequest));
							}
						}
					}
					if (projectRequest.addRequires) {
						dependencyClosure.dependencies(always -> List.of())//
								.filter(entry -> entry.getValue().stream()//
										.flatMap(dependency -> dependencyClosure.getProject(dependency).stream())//
										.anyMatch(projectRequest::matches))//
								.map(Entry::getKey)//
								.distinct()//
								.peek(project -> {
									if (DEBUG) {
										log.info(" + add project '" + project.getId() + "' that depends on '"
												+ projectRequest.mavenProject.getId() + "'...");
									}
								})//
								// request dependencies of dependants, otherwise, -amd would not be able to
								// produce a satisfiable build graph
								.forEach(project -> queue.add(new ProjectRequest(project, true, true, projectRequest)));
					}
				}
			}
			// add target projects always, they don't really add to the build times but are
			// needed if referenced inside projects we might be more selective and choose
			// target projects depending on project configuration
			for (MavenProject mavenProject : projects) {
				if (PackagingType.TYPE_ECLIPSE_TARGET_DEFINITION.equals(mavenProject.getPackaging())) {
					selectedProjects.add(mavenProject);
				}

			}
		} finally {
			executor.ifPresent(ExecutorService::shutdownNow);
		}

		try {
			log.debug("=============== Selected Projects ==================");

			selectedProjects.stream()
					.sorted(Comparator.comparing(MavenProject::getGroupId, String.CASE_INSENSITIVE_ORDER)
							.thenComparing(MavenProject::getArtifactId, String.CASE_INSENSITIVE_ORDER))
					.forEachOrdered(p -> log.debug(p.getId()));
			return Result.success(new DefaultProjectDependencyGraph(projects, selectedProjects));
		} catch (DuplicateProjectException | CycleDetectedException e) {
			log.error("Cannot compute project's dependency graph", e);
			return Result.error(graph);
		}
	}

	private String getProjectKey(Dependency project) {
		return project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion();
	}

	private String getProjectKey(MavenProject project) {
		return project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion();
	}

	private List<ModelProblem> toProblems(IStatus status, List<ModelProblem> problems) {
		int severity = status.getSeverity();
		if (severity == IStatus.OK || severity == IStatus.INFO) {
			return problems;
		}
		Throwable throwable = status.getException();
		Exception exception;
		if (throwable == null) {
			exception = null;
		} else if (throwable instanceof Exception ex) {
			exception = ex;
		} else {
			exception = new ExecutionException(throwable);
		}
		Severity serv;
		if (severity == IStatus.WARNING || severity == IStatus.CANCEL) {
			serv = Severity.WARNING;
		} else {
			serv = Severity.ERROR;
		}
		problems.add(new DefaultModelProblem(status.getMessage(), serv, null, null, 0, 0, exception));
		for (IStatus child : status.getChildren()) {
			toProblems(child, problems);
		}
		return problems;
	}

	private static final class ProjectRequest {

		final MavenProject mavenProject;
		final ProjectRequest parent;
		/**
		 * The request is to add everything that requires the given maven project
		 */
		final boolean addRequires;
		/**
		 * The request is to add any dependency of the given maven project
		 */
		final boolean addDependencies;

		ProjectRequest(MavenProject mavenProject, boolean addRequires, boolean addDependencies, ProjectRequest parent) {
			this.addRequires = addRequires;
			this.addDependencies = addDependencies;
			this.parent = parent;
			this.mavenProject = mavenProject;
		}

		boolean matches(MavenProject mavenproject) {
			return this.mavenProject == mavenproject;
		}

		@Override
		public String toString() {
			return "ProjectRequest [mavenProject=" + mavenProject + ", parent=" + parent + ", addRequires="
					+ addRequires + ", addDependencies=" + addDependencies + "]";
		}
	}

}
