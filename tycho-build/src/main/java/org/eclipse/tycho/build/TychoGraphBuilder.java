/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation based on 
 *    		org.eclipse.tycho.p2.util.resolution.AbstractSlicerResolutionStrategy
 *    		org.eclipse.tycho.p2.util.resolution.ProjectorResolutionStrategy
 *******************************************************************************/
package org.eclipse.tycho.build;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.DirectorActivator;
import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureParser;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.target.ee.NoExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.p2.util.resolution.ProjectorResolutionStrategy;
import org.eclipse.tycho.p2.util.resolution.ResolutionDataImpl;
import org.eclipse.tycho.p2.util.resolution.ResolverException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

@Component(role = GraphBuilder.class, hint = GraphBuilder.HINT)
public class TychoGraphBuilder extends DefaultGraphBuilder {

	public static final String TYPE_ECLIPSE_PLUGIN = "eclipse-plugin";
	public static final String TYPE_ECLIPSE_TEST_PLUGIN = "eclipse-test-plugin";
	public static final String TYPE_ECLIPSE_FEATURE = "eclipse-feature";
	public static final String TYPE_ECLIPSE_REPOSITORY = "eclipse-repository";
	public static final String TYPE_ECLIPSE_TARGET_DEFINITION = "eclipse-target-definition";

	@Requirement
	private Logger log;

	@Requirement(hint = "plexus")
	private BundleContext bundleContext;

	@Override
	public Result<ProjectDependencyGraph> build(MavenSession session) {
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
		if (DirectorActivator.context == null) {
			DirectorActivator.context = bundleContext;
		}
		ProjectDependencyGraph graph = graphResult.get();
		List<MavenProject> projects = graph.getAllProjects();
		List<ModelProblem> problems = new CopyOnWriteArrayList<>();
		int degreeOfConcurrency = request.getDegreeOfConcurrency();
		boolean failFast = MavenExecutionRequest.REACTOR_FAIL_FAST.equals(request.getReactorFailureBehavior());
		Optional<ExecutorService> executor;
		if (degreeOfConcurrency > 1) {
			executor = Optional.of(new ForkJoinPool(degreeOfConcurrency));
		} else {
			executor = Optional.empty();
		}
		BooleanSupplier hasFailures = () -> failFast && !problems.isEmpty();
		Set<MavenProject> selectedProjects = ConcurrentHashMap.newKeySet();
		try {
			Map<MavenProject, Collection<IInstallableUnit>> projectIUMap = new ConcurrentHashMap<>();
			try {
				var projectUnits = computeProjectUnits(problems);
				runStream(projects.stream(), p -> projectIUMap.put(p, projectUnits.apply(p)), hasFailures, executor);
			} catch (ExecutionException e) {
				log.error("Can't read projects", e);
				return Result.error(graph);
			}
			Map<MavenProject, Collection<IInstallableUnit>> projectDependenciesMap = new ConcurrentHashMap<MavenProject, Collection<IInstallableUnit>>();
			try {
				Collection<IInstallableUnit> availableIUs = projectIUMap.values().stream().flatMap(Collection::stream)
						.collect(Collectors.toSet());
				var projectDependencies = computeProjectDependencies(availableIUs, problems, loggerAdapter);
				runStream(projectIUMap.entrySet().stream(),
						entry -> projectDependenciesMap.put(entry.getKey(), projectDependencies.apply(entry)),
						hasFailures, executor);
			} catch (ExecutionException e) {
				log.error("Can't resolve projects", e);
				return Result.error(graph);
			}
			if (hasFailures.getAsBoolean()) {
				if (loggerAdapter.isExtendedDebugEnabled()) {
					for (ModelProblem modelProblem : problems) {
						loggerAdapter.error(modelProblem.getMessage(), modelProblem.getException());
					}
				}
				return Result.error(graph, problems);
			}
			Map<IInstallableUnit, MavenProject> iuProjectMap = new HashMap<IInstallableUnit, MavenProject>();
			for (var entry : projectIUMap.entrySet()) {
				MavenProject mavenProject = entry.getKey();
				for (IInstallableUnit iu : entry.getValue()) {
					iuProjectMap.put(iu, mavenProject);
				}
			}
			if (loggerAdapter.isExtendedDebugEnabled()) {
				for (Entry<MavenProject, Collection<IInstallableUnit>> entry : projectDependenciesMap.entrySet()) {
					MavenProject project = entry.getKey();
					Collection<IInstallableUnit> depends = entry.getValue();
					if (depends.isEmpty()) {
						continue;
					}
					loggerAdapter.debug("[[ project " + project.getName() + " depends on: ]]");
					for (IInstallableUnit dependency : depends) {
						MavenProject mavenProject = iuProjectMap.get(dependency);
						if (mavenProject == null) {
							loggerAdapter.debug(" IU: " + dependency);
						} else {
							loggerAdapter.debug(" IU: " + dependency + " [of project " + mavenProject.getName() + "]");
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
						Collection<IInstallableUnit> depends = projectDependenciesMap
								.getOrDefault(projectRequest.mavenProject, Collections.emptyList());
						depends.stream()//
								.map(iuProjectMap::get)//
								.filter(Objects::nonNull)//
								.distinct()//
								.peek(project -> loggerAdapter.debug(" + add upstream project '" + project.getName()
										+ "' of project '" + projectRequest.mavenProject.getName() + "'..."))//
								// make behaviors are both false here as projectDependenciesMap includes transitive already
								.forEach(
										project -> queue.add(new ProjectRequest(project, false, false, projectRequest)));
					}
					if (projectRequest.requestDownstream) {
						projectDependenciesMap.entrySet().stream()//
								.filter(entry -> {
									return entry.getValue().stream()//
											.map(iuProjectMap::get)//
											.filter(Objects::nonNull)//
											.anyMatch(projectRequest::matches);
								})//
								.map(Entry::getKey)//
								.distinct()//
								.peek(project -> loggerAdapter.debug(" + add downstream project '" + project.getName()
										+ "' of project '" + projectRequest.mavenProject.getName() + "'..."))//
								// request dependencies of dependants, otherwise, -amd would not be able to produce a satisfiable build graph
								.forEach(
										project -> queue.add(new ProjectRequest(project, false, true, projectRequest)));
					}
				}
			}
			// add target projects always, they don't really add to the build times but are
			// needed if referenced inside projects we might be more selective and choose
			// target projects depending on project configuration
			for (MavenProject mavenProject : projects) {
				if (TYPE_ECLIPSE_TARGET_DEFINITION.equals(mavenProject.getPackaging())) {
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

	private Function<MavenProject, Collection<IInstallableUnit>> computeProjectUnits(List<ModelProblem> problems) {
		return project -> {
			PublisherInfo publisherInfo = new PublisherInfo();
			publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX);
			if (TYPE_ECLIPSE_PLUGIN.equals(project.getPackaging())
					|| TYPE_ECLIPSE_TEST_PLUGIN.equals(project.getPackaging())) {
				try {
					BundleDescription bundleDescription = BundlesAction.createBundleDescription(project.getBasedir());
					IArtifactKey descriptor = BundlesAction.createBundleArtifactKey(bundleDescription.getSymbolicName(),
							bundleDescription.getVersion().toString());
					IInstallableUnit iu = BundlesAction.createBundleIU(bundleDescription, descriptor, publisherInfo);
					return Collections.singletonList(iu);
				} catch (IOException | BundleException e) {
					problems.add(new DefaultModelProblem(
							"can't read " + project.getPackaging() + " project @ " + project.getBasedir(),
							Severity.ERROR, null, null, 0, 0, e));
				}
			} else if (TYPE_ECLIPSE_FEATURE.equals(project.getPackaging())) {
				FeatureParser parser = new FeatureParser();
				File basedir = project.getBasedir();
				Feature feature = parser.parse(basedir);
				Map<IInstallableUnit, Feature> featureMap = new HashMap<>();
				FeaturesAction action = new FeaturesAction(new Feature[] { feature }) {
					@Override
					protected void publishFeatureArtifacts(Feature feature, IInstallableUnit featureIU,
							IPublisherInfo publisherInfo) {
						// so not call super as we don't wan't to copy anything --> Bug in P2 with
						// IPublisherInfo.A_INDEX option
						// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=578380
					}

					@Override
					protected IInstallableUnit generateFeatureJarIU(Feature feature, IPublisherInfo publisherInfo) {
						IInstallableUnit iu = super.generateFeatureJarIU(feature, publisherInfo);
						featureMap.put(iu, feature);
						return iu;
					}
				};
				PublisherResult results = new PublisherResult();
				action.perform(publisherInfo, results, null);
				Set<IInstallableUnit> result = results.query(QueryUtil.ALL_UNITS, null).toSet();
				return result;

			}
			return Collections.emptyList();
		};
	}

	private Function<Entry<MavenProject, Collection<IInstallableUnit>>, Collection<IInstallableUnit>> computeProjectDependencies(
			Collection<IInstallableUnit> availableIUs, List<ModelProblem> problems, MavenLogger logger) {
		return entry -> {
			Collection<IInstallableUnit> projectUnits = entry.getValue();
			if (!projectUnits.isEmpty()) {
				MavenProject project = entry.getKey();
				logger.debug("Resolve dependencies for project " + project.getName() + "...");
				try {
					ProjectorResolutionStrategy resolutionStrategy = new ProjectorResolutionStrategy(logger);
					ResolutionDataImpl data = new ResolutionDataImpl(NoExecutionEnvironmentResolutionHints.INSTANCE);
					data.setFailOnMissing(false);
					data.setAvailableIUs(Collections.unmodifiableCollection(availableIUs));
					data.setRootIUs(Collections.unmodifiableCollection(projectUnits));
					data.setSlicerPredicate(always -> true);
					resolutionStrategy.setData(data);
					Collection<IInstallableUnit> resolve = resolutionStrategy.resolve(new HashMap<String, String>(),
							new NullProgressMonitor());
					resolve.removeAll(projectUnits);
					return resolve;
				} catch (ResolverException e) {
					problems.add(new DefaultModelProblem(
							"can't resolve " + project.getPackaging() + " project @ " + project.getBasedir(),
							Severity.ERROR, null, null, 0, 0, e));
				}
			}
			return Collections.emptyList();
		};
	}

	private static <T> void runStream(Stream<T> stream, Consumer<? super T> consumer, BooleanSupplier hasFailures,
			Optional<ExecutorService> service) throws ExecutionException {
		if (hasFailures.getAsBoolean()) {
			// short-cut
			return;
		}
		Predicate<T> takeWhile = nil -> !hasFailures.getAsBoolean();
		if (service.isEmpty()) {
			stream.unordered().takeWhile(takeWhile).forEach(consumer);
		} else {
			Future<?> future = service.get().submit(() -> {
				stream.unordered().parallel().takeWhile(takeWhile).forEach(consumer);
			});
			try {
				future.get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
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
