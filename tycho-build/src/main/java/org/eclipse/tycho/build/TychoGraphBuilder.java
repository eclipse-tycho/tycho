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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.graph.DefaultGraphBuilder;
import org.apache.maven.graph.DefaultProjectDependencyGraph;
import org.apache.maven.graph.GraphBuilder;
import org.apache.maven.model.building.DefaultModelProblem;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.Result;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2maven.MavenProjectDependencyProcessor;
import org.eclipse.tycho.p2maven.MavenProjectDependencyProcessor.ProjectDependencies;
import org.eclipse.tycho.p2maven.MavenProjectDependencyProcessor.ProjectDependencyClosure;
import org.eclipse.tycho.pomless.AbstractTychoMapping;
import org.sonatype.maven.polyglot.mapping.Mapping;

@Component(role = GraphBuilder.class, hint = GraphBuilder.HINT)
public class TychoGraphBuilder extends DefaultGraphBuilder {

	@Requirement
	private Logger log;

	@Requirement(role = Mapping.class)
	private Map<String, Mapping> polyglotMappings;

	@Requirement
	private MavenProjectDependencyProcessor dependencyProcessor;

	@Override
	public Result<ProjectDependencyGraph> build(MavenSession session) {
		Objects.requireNonNull(session);
		// Tell the polyglot mappings that we are in extension mode
		for (Mapping mapping : polyglotMappings.values()) {
			if (mapping instanceof AbstractTychoMapping tychoMapping) {
				tychoMapping.setExtensionMode(true);
				tychoMapping.setMultiModuleProjectDirectory(session.getRequest().getMultiModuleProjectDirectory());
				if (session.getRequest().getSystemProperties().getProperty("tycho.buildqualifier.format") != null) {
					tychoMapping.setSnapshotFormat("${" + TychoCiFriendlyVersions.BUILD_QUALIFIER + "}");
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
		session.getUserProperties().put("tycho.mode", "extension");
		MavenLogger loggerAdapter = new MavenLoggerAdapter(log,
				Boolean.valueOf(session.getUserProperties().getProperty("tycho.debug.resolver")));
		String makeBehavior = request.getMakeBehavior();
		if (loggerAdapter.isExtendedDebugEnabled()) {
			loggerAdapter.debug("TychoGraphBuilder:");
			loggerAdapter.debug("  - SelectedProjects: " + request.getSelectedProjects());
			loggerAdapter.debug("  - ExcludedProjects: " + request.getExcludedProjects());
			loggerAdapter.debug("  - MakeBehavior:     " + makeBehavior);
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
				log.error("Can't resolve projects", e);
				return Result.error(graph, toProblems(e.getStatus(), new ArrayList<>()));
			}

			if (loggerAdapter.isExtendedDebugEnabled()) {
				for (MavenProject project : projects) {
					ProjectDependencies depends = dependencyClosure.getProjectDependecies(project);
					if (depends.getDependencies().isEmpty()) {
						continue;
					}
					loggerAdapter.debug("[[ project " + project.getName() + " depends on: ]]");
					for (IInstallableUnit dependency : depends.getDependencies()) {
						Optional<MavenProject> mavenProject = dependencyClosure.getProject(dependency);
						if (mavenProject.isEmpty()) {
							loggerAdapter.debug(" IU: " + dependency);
						} else {
							loggerAdapter
									.debug(" IU: " + dependency + " [of project " + mavenProject.get().getName() + "]");
						}
					}
				}
			}
			Queue<ProjectRequest> queue = new ConcurrentLinkedQueue<>(graph.getSortedProjects().stream()
					.map(p -> new ProjectRequest(p, makeDownstream, makeUpstream, null)).toList());
			loggerAdapter.debug("Computing additional " + makeBehavior
					+ " dependencies based on initial project set of " + queue.stream().map(r -> r.mavenProject)
							.map(MavenProject::getName).collect(Collectors.joining(", ")));
			while (!queue.isEmpty()) {
				ProjectRequest projectRequest = queue.poll();
				if (selectedProjects.add(projectRequest.mavenProject)) {
					if (projectRequest.addDependencies) {
						dependencyClosure.getDependencyProjects(projectRequest.mavenProject).forEach(project -> {
							loggerAdapter.debug(" + add dependency project '" + project.getId() + "' of project '"
									+ projectRequest.mavenProject.getId() + "'...");
							// we also need to add the dependencies of the dependency project
							queue.add(new ProjectRequest(project, false, true, projectRequest));
						});
					}
					if (projectRequest.addRequires) {
						dependencyClosure.dependencies()//
								.filter(entry -> 
									entry.getValue().stream()//
											.flatMap(dependency -> dependencyClosure.getProject(dependency).stream())//
											.anyMatch(projectRequest::matches)
								)//
								.map(Entry::getKey)//
								.distinct()//
								.peek(project -> loggerAdapter.debug(" + add project '" + project.getId()
										+ "' that depends on '" + projectRequest.mavenProject.getId() + "'..."))//
								// request dependencies of dependants, otherwise, -amd would not be able to
								// produce a satisfiable build graph
								.forEach(
										project -> queue.add(new ProjectRequest(project, true, true, projectRequest)));
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
			log.debug("=============== SELECTED PROJECTS ==================");

			selectedProjects.stream()
					.sorted(Comparator.comparing(MavenProject::getGroupId, String.CASE_INSENSITIVE_ORDER)
							.thenComparing(MavenProject::getArtifactId, String.CASE_INSENSITIVE_ORDER))
					.forEachOrdered(p -> log.debug(p.getId()));
			return Result.success(new DefaultProjectDependencyGraph(projects, selectedProjects));
		} catch (DuplicateProjectException | CycleDetectedException e) {
			log.error("Can't compute project dependency graph", e);
			return Result.error(graph);
		}
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

		ProjectRequest(MavenProject mavenProject, boolean addRequires, boolean addDependencies,
				ProjectRequest parent) {
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
