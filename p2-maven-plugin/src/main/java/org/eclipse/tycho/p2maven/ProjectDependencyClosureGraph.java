/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.tycho.p2maven.MavenProjectDependencyProcessor.ProjectDependencies;
import org.eclipse.tycho.p2maven.MavenProjectDependencyProcessor.ProjectDependencyClosure;
import org.eclipse.tycho.p2maven.tmp.BundlesAction;

class ProjectDependencyClosureGraph implements ProjectDependencyClosure {

	/**
	 * Represents a requirement from an installable unit
	 */
	record Requirement(IInstallableUnit installableUnit, IRequirement requirement) {
	}

	/**
	 * Represents a capability provided by an installable unit
	 */
	record Capability(MavenProject project, IInstallableUnit installableUnit) {
	}

	/**
	 * Represents a directional edge in the dependency graph
	 */
	record Edge(Requirement requirement, Capability capability) {
	}

	private static final ProjectDependencies EMPTY_DEPENDENCIES = new ProjectDependencies(Map.of(), Set.of());

	private static final boolean DUMP_DATA = Boolean.getBoolean("tycho.p2.dump")
			|| Boolean.getBoolean("tycho.p2.dump.dependencies");

	private final Map<IInstallableUnit, MavenProject> iuProjectMap = new HashMap<>();

	private final Map<MavenProject, ProjectDependencies> projectDependenciesMap = new ConcurrentHashMap<>();

	Map<MavenProject, Collection<IInstallableUnit>> projectIUMap;

	// Graph structure: maps each project to its outgoing edges
	Map<MavenProject, List<Edge>> projectEdgesMap = new HashMap<>();

	ProjectDependencyClosureGraph(Map<MavenProject, Collection<IInstallableUnit>> projectIUMap) throws CoreException {
		this.projectIUMap = projectIUMap;
		// Build IU to project mapping
		for (var entry : projectIUMap.entrySet()) {
			MavenProject mavenProject = entry.getKey();
			for (IInstallableUnit iu : entry.getValue()) {
				iuProjectMap.put(iu, mavenProject);
			}
		}
		// Build the graph structure
		buildGraph();
		// Write DOT file if requested
		if (DUMP_DATA) {
			try {
				DotDump.dump(new File("project-dependencies.dot"), this);
			} catch (IOException e) {
				// Ignore dump errors
			}
		}
	}

	/**
	 * Build the internal graph structure based on projectIUMap.
	 * For each project, we collect all requirements from all IUs provided by that project,
	 * then create edges to the capabilities that satisfy those requirements.
	 */
	private void buildGraph() {
		// Pre-create Capability objects for all IUs to prevent multiple object creation
		List<Capability> allCapabilities = projectIUMap.entrySet().stream()
				.flatMap(entry -> entry.getValue().stream()
						.map(iu -> new Capability(entry.getKey(), iu)))
				.collect(Collectors.toList());

		// Build edges for each project in parallel
		Map<MavenProject, List<Edge>> result = new java.util.concurrent.ConcurrentHashMap<>();
		
		projectIUMap.entrySet().parallelStream().unordered().forEach(entry -> {
			MavenProject project = entry.getKey();
			List<Edge> edges = new ArrayList<>();
			Collection<IInstallableUnit> projectUnits = entry.getValue();

			// Collect all requirements from all IUs of this project (excluding MetaRequirements)
			// Include all requirements, even self-satisfied ones (allowing cyclic dependencies)
			Set<Requirement> requirements = new LinkedHashSet<>();
			for (IInstallableUnit iu : projectUnits) {
				for (IRequirement req : iu.getRequirements()) {
					requirements.add(new Requirement(iu, req));
				}
			}

			// For each requirement, find matching capabilities and create edges
			for (Requirement requirement : requirements) {
				// Search through all capabilities to find those that satisfy the requirement
				for (Capability capability : allCapabilities) {
					if (capability.installableUnit.satisfies(requirement.requirement)) {
						edges.add(new Edge(requirement, capability));
					}
				}
			}

			result.put(project, edges);
		});
		
		projectEdgesMap.putAll(result);
	}

	
	/**
	 * Detect all cycles in the dependency graph using Tarjan's algorithm for strongly connected components
	 * 
	 * @return a set of sets, where each inner set represents a cycle (strongly connected component with more than one node)
	 */
	Set<Set<MavenProject>> detectCycles() {
		Set<Set<MavenProject>> cycles = new HashSet<>();
		Map<MavenProject, Integer> index = new HashMap<>();
		Map<MavenProject, Integer> lowLink = new HashMap<>();
		Map<MavenProject, Boolean> onStack = new HashMap<>();
		List<MavenProject> stack = new ArrayList<>();
		int[] indexCounter = {0};
		
		for (MavenProject project : projectIUMap.keySet()) {
			if (!index.containsKey(project)) {
				strongConnect(project, index, lowLink, onStack, stack, indexCounter, cycles);
			}
		}
		
		return cycles;
	}
	
	/**
	 * Tarjan's strongly connected components algorithm
	 */
	private void strongConnect(MavenProject v, Map<MavenProject, Integer> index, 
			Map<MavenProject, Integer> lowLink, Map<MavenProject, Boolean> onStack,
			List<MavenProject> stack, int[] indexCounter, Set<Set<MavenProject>> cycles) {
		
		index.put(v, indexCounter[0]);
		lowLink.put(v, indexCounter[0]);
		indexCounter[0]++;
		stack.add(v);
		onStack.put(v, true);
		
		// Consider successors of v
		List<Edge> edges = projectEdgesMap.getOrDefault(v, List.of());
		for (Edge edge : edges) {
			MavenProject w = edge.capability.project;
			
			if (!index.containsKey(w)) {
				// Successor w has not yet been visited; recurse on it
				strongConnect(w, index, lowLink, onStack, stack, indexCounter, cycles);
				lowLink.put(v, Math.min(lowLink.get(v), lowLink.get(w)));
			} else if (onStack.getOrDefault(w, false)) {
				// Successor w is in stack and hence in the current SCC
				lowLink.put(v, Math.min(lowLink.get(v), index.get(w)));
			}
		}
		
		// If v is a root node, pop the stack and generate an SCC
		if (lowLink.get(v).equals(index.get(v))) {
			Set<MavenProject> scc = new HashSet<>();
			MavenProject w;
			do {
				w = stack.remove(stack.size() - 1);
				onStack.put(w, false);
				scc.add(w);
			} while (!w.equals(v));
			
			// Only add if it's a real cycle (more than one node, or has self-edge)
			if (scc.size() > 1 || hasSelfEdge(v)) {
				cycles.add(scc);
			}
		}
	}
	
	/**
	 * Check if a project has a self-referencing edge
	 */
	private boolean hasSelfEdge(MavenProject project) {
		List<Edge> edges = projectEdgesMap.getOrDefault(project, List.of());
		for (Edge edge : edges) {
			if (edge.capability.project.equals(project)) {
				return true;
			}
		}
		return false;
	}
	

	@Override
	public Optional<MavenProject> getProject(IInstallableUnit installableUnit) {
		return Optional.ofNullable(iuProjectMap.get(installableUnit));
	}

	@Override
	public ProjectDependencies getProjectDependecies(MavenProject mavenProject) {
		return projectDependenciesMap.computeIfAbsent(mavenProject, project -> computeProjectDependencies(project));
	}

	private ProjectDependencies computeProjectDependencies(MavenProject project) {
		Collection<IInstallableUnit> projectUnits = projectIUMap.get(project);
		if (projectUnits != null) {
			// Build requirements map from edges
			// Group edges by requirement and collect all satisfying IUs
			Map<IRequirement, Collection<IInstallableUnit>> requirementsMap = new LinkedHashMap<>();
			List<Edge> edges = projectEdgesMap.getOrDefault(project, List.of());

			for (Edge edge : edges) {
				// Only add if not from the same project (use project from capability)
				if (!edge.capability.project.equals(project)) {
					requirementsMap.computeIfAbsent(edge.requirement.requirement, k -> new ArrayList<>())
							.add(edge.capability.installableUnit);
				}
			}
			return new ProjectDependencies(requirementsMap, Set.copyOf(projectUnits));
		}
		return EMPTY_DEPENDENCIES;
	}

	@Override
	public Stream<Entry<MavenProject, Collection<IInstallableUnit>>> dependencies(
			Function<MavenProject, Collection<IInstallableUnit>> contextIuSupplier) {
		return projectIUMap.keySet().stream().map(mavenProject -> {
			ProjectDependencies projectDependencies = getProjectDependecies(mavenProject);
			Collection<IInstallableUnit> dependentUnits = projectDependencies
					.getDependencies(contextIuSupplier.apply(mavenProject));
			return new SimpleEntry<>(mavenProject, dependentUnits);
		});
	}

	@Override
	public boolean isFragment(MavenProject mavenProject) {

		return getProjectUnits(mavenProject).stream().anyMatch(ProjectDependencyClosureGraph::isFragment);
	}

	@Override
	public Collection<IInstallableUnit> getProjectUnits(MavenProject mavenProject) {
		Collection<IInstallableUnit> collection = projectIUMap.get(mavenProject);
		if (collection != null) {
			return collection;
		}
		return Collections.emptyList();
	}

	@Override
	public Collection<MavenProject> getDependencyProjects(MavenProject mavenProject,
			Collection<IInstallableUnit> contextIUs) {
		ProjectDependencies projectDependecies = getProjectDependecies(mavenProject);
		List<MavenProject> list = projectDependecies.getDependencies(contextIUs).stream()
				.flatMap(dependency -> getProject(dependency).stream()).distinct().toList();
		if (isFragment(mavenProject)) {
			// for projects that are fragments don't do any special processing...
			return list;
		}
		// for regular projects we must check if they have any fragment requirements
		// that must be attached here, example is SWT that defines a requirement to its
		// fragments and if build inside the same reactor with a consumer (e.g. JFace)
		// has to be applied
		return list.stream().flatMap(project -> {
			ProjectDependencies dependecies = getProjectDependecies(project);
			return Stream.concat(Stream.of(project),
					dependecies.getDependencies(contextIUs).stream()
							.filter(dep -> hasAnyHost(dep, dependecies.projectUnits))
							.flatMap(dependency -> getProject(dependency).stream()));
		}).toList();
	}

	private static boolean isFragment(IInstallableUnit installableUnit) {
		return getFragmentCapability(installableUnit).findAny().isPresent();
	}

	private static Stream<IProvidedCapability> getFragmentCapability(IInstallableUnit installableUnit) {

		return installableUnit.getProvidedCapabilities().stream()
				.filter(cap -> BundlesAction.CAPABILITY_NS_OSGI_FRAGMENT.equals(cap.getNamespace()));
	}

	private static boolean hasAnyHost(IInstallableUnit unit, Iterable<IInstallableUnit> collection) {
		return getFragmentHostRequirement(unit).anyMatch(req -> {
			for (IInstallableUnit iu : collection) {
				if (req.isMatch(iu)) {
					return true;
				}
			}
			return false;
		});
	}
	
	private static Stream<IRequirement> getFragmentHostRequirement(IInstallableUnit installableUnit) {
		return getFragmentCapability(installableUnit).map(provided -> {
			String hostName = provided.getName();
			for (IRequirement requirement : installableUnit.getRequirements()) {
				if (requirement instanceof IRequiredCapability requiredCapability) {
					if (hostName.equals(requiredCapability.getName())) {
						return requirement;
					}
				}
			}
			return null;
		}).filter(Objects::nonNull);
	}

}
