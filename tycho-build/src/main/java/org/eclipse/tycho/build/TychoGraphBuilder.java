/*******************************************************************************
 * Copyright (c) 2021, 2022 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation based on 
 *    		org.eclipse.tycho.p2.util.resolution.AbstractSlicerResolutionStrategy
 *    		org.eclipse.tycho.p2.util.resolution.ProjectorResolutionStrategy
 *******************************************************************************/
package org.eclipse.tycho.build;

import java.util.ArrayList;
import java.util.Collection;
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
import org.eclipse.tycho.p2maven.InstallableUnitGenerator;
import org.eclipse.tycho.p2maven.MavenProjectDependencyProcessor;
import org.eclipse.tycho.p2maven.MavenProjectDependencyProcessor.ProjectDependencyClosure;
import org.eclipse.tycho.pomless.AbstractTychoMapping;
import org.osgi.framework.BundleContext;
import org.sonatype.maven.polyglot.mapping.Mapping;

@Component(role = GraphBuilder.class, hint = GraphBuilder.HINT)
public class TychoGraphBuilder extends DefaultGraphBuilder {

	@Requirement
	private Logger log;

	@Requirement(hint = "plexus")
	private BundleContext bundleContext;

	@Requirement(role = Mapping.class)
	private Map<String, Mapping> polyglotMappings;

	@Requirement
	private InstallableUnitGenerator generator;

	@Requirement
	private MavenProjectDependencyProcessor dependencyProcessor;

	@Override
	public Result<ProjectDependencyGraph> build(MavenSession session) {
		Objects.requireNonNull(session);
		// Tell the polyglot mappings that we are in extension mode
		for (Mapping mapping : polyglotMappings.values()) {
			if (mapping instanceof AbstractTychoMapping) {
				AbstractTychoMapping tychoMapping = (AbstractTychoMapping) mapping;
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
		boolean makeUpstream = MavenExecutionRequest.REACTOR_MAKE_UPSTREAM.equals(makeBehavior)
				|| MavenExecutionRequest.REACTOR_MAKE_BOTH.equals(makeBehavior);
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
					Collection<IInstallableUnit> depends = dependencyClosure.getProjectDependecies(project);
					if (depends.isEmpty()) {
						continue;
					}
					loggerAdapter.debug("[[ project " + project.getName() + " depends on: ]]");
					for (IInstallableUnit dependency : depends) {
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
					.map(p -> new ProjectRequest(p, makeDownstream, makeUpstream, null)).collect(Collectors.toList()));
			loggerAdapter.debug("Computing additional " + makeBehavior
					+ " dependencies based on initial project set of " + queue.stream().map(r -> r.mavenProject)
							.map(MavenProject::getName).collect(Collectors.joining(", ")));
			while (!queue.isEmpty()) {
				ProjectRequest projectRequest = queue.poll();
				if (selectedProjects.add(projectRequest.mavenProject)) {
					if (projectRequest.requestUpstream) {
						dependencyClosure.getDependencyProjects(projectRequest.mavenProject).forEach(project -> {
							loggerAdapter.debug(" + add upstream project '" + project.getName() + "' of project '"
									+ projectRequest.mavenProject.getName() + "'...");
							// make behaviors are both false here as projectDependenciesMap includes
							// transitive already
							queue.add(new ProjectRequest(project, false, false, projectRequest));
						});
					}
					if (projectRequest.requestDownstream) {
						dependencyClosure.dependencies()//
								.filter(entry -> {
									return entry.getValue().stream()//
											.flatMap(dependency -> dependencyClosure.getProject(dependency).stream())//
											.anyMatch(projectRequest::matches);
								})//
								.map(Entry::getKey)//
								.distinct()//
								.peek(project -> loggerAdapter.debug(" + add downstream project '" + project.getName()
										+ "' of project '" + projectRequest.mavenProject.getName() + "'..."))//
								// request dependencies of dependants, otherwise, -amd would not be able to
								// produce a satisfiable build graph
								.forEach(
										project -> queue.add(new ProjectRequest(project, false, true, projectRequest)));
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
		} else if (throwable instanceof Exception) {
			exception = (Exception) throwable;
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
		final boolean requestDownstream;
		final boolean requestUpstream;

		ProjectRequest(MavenProject mavenProject, boolean requestDownstream, boolean requestUpstream,
				ProjectRequest parent) {
			this.requestDownstream = requestDownstream;
			this.requestUpstream = requestUpstream;
			this.parent = parent;
			this.mavenProject = mavenProject;
		}

		boolean matches(MavenProject mavenproject) {
			return this.mavenProject == mavenproject;
		}

		@Override
		public String toString() {
			return "ProjectRequest [mavenProject=" + mavenProject + ", parent=" + parent + ", requestDownstream="
					+ requestDownstream + ", requestUpstream=" + requestUpstream + "]";
		}
	}

}
