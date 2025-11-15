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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.query.CollectionResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.tycho.p2maven.MavenProjectDependencyProcessor.ProjectDependencies;
import org.eclipse.tycho.p2maven.MavenProjectDependencyProcessor.ProjectDependencyClosure;
import org.eclipse.tycho.p2maven.io.MetadataIO;
import org.eclipse.tycho.p2maven.tmp.BundlesAction;

class ProjectDependencyClosureGraph implements ProjectDependencyClosure {

	private static final ProjectDependencies EMPTY_DEPENDENCIES = new ProjectDependencies(Map.of(), Set.of());

	private static final boolean DUMP_DATA = Boolean.getBoolean("tycho.p2.dump")
			|| Boolean.getBoolean("tycho.p2.dump.dependencies");

	private final Map<IInstallableUnit, MavenProject> iuProjectMap = new HashMap<>();

	private Map<MavenProject, ProjectDependencies> projectDependenciesMap;

	private InstallableUnitSlicer slicer;

	private Map<MavenProject, Collection<IInstallableUnit>> projectIUMap;

	ProjectDependencyClosureGraph(Map<MavenProject, Collection<IInstallableUnit>> projectIUMap,
			InstallableUnitSlicer slicer) throws CoreException {
		this.projectIUMap = projectIUMap;
		this.slicer = slicer;
		Collection<IInstallableUnit> availableIUs = projectIUMap.values().stream().flatMap(Collection::stream)
				.collect(Collectors.toSet());
		projectDependenciesMap = computeProjectDependencies(
				projectIUMap.keySet(), new CollectionResult<>(availableIUs), projectIUMap);
		for (var entry : projectIUMap.entrySet()) {
			MavenProject mavenProject = entry.getKey();
			for (IInstallableUnit iu : entry.getValue()) {
				iuProjectMap.put(iu, mavenProject);
			}
		}
	}

	/**
	 * Given a set of projects, compute the mapping of a project to its dependencies
	 * 
	 * @param projects    the projects to investigate
	 * @param avaiableIUs all available units that should be used to fulfill project
	 *                    requirements
	 * @return a Map from the passed projects to their dependencies
	 * @throws CoreException if computation failed
	 */
	private Map<MavenProject, ProjectDependencies> computeProjectDependencies(Collection<MavenProject> projects,
			IQueryable<IInstallableUnit> avaiableIUs, Map<MavenProject, Collection<IInstallableUnit>> projectIUMap)
			throws CoreException {
		List<CoreException> errors = new CopyOnWriteArrayList<>();
		Map<MavenProject, ProjectDependencies> result = new ConcurrentHashMap<>();
		projects.parallelStream().unordered().takeWhile(nil -> errors.isEmpty()).forEach(project -> {
			try {
				ProjectDependencies projectDependencies = computeProjectDependencies(
						Set.copyOf(projectIUMap.get(project)), avaiableIUs);
				result.put(project, projectDependencies);
				if (DUMP_DATA) {
					File file = new File(project.getBasedir(), "project-dependencies.xml");
					try {
						new MetadataIO().writeXML(
								Collections.unmodifiableCollection(projectDependencies.getDependencies(List.of())),
								file);
					} catch (IOException e) {
					}
				}
			} catch (CoreException e) {
				errors.add(e);
			}
		});
		if (errors.isEmpty()) {
			return result;
		}
		if (errors.size() == 1) {
			throw errors.get(0);
		}
		MultiStatus multiStatus = new MultiStatus(InstallableUnitGenerator.class, IStatus.ERROR,
				"computing installable unit units failed");
		errors.forEach(e -> multiStatus.add(e.getStatus()));
		throw new CoreException(multiStatus);
	}

	/**
	 * Given a {@link MavenProject} and a collection of {@link IInstallableUnit},
	 * compute the collection of dependencies that fulfill the projects requirements
	 * 
	 * @param project     the project to query for requirements
	 * @param avaiableIUs all available units that should be used to fulfill project
	 *                    requirements
	 * @return the collection of dependent {@link InstallableUnit}s
	 * @throws CoreException if computation failed
	 */
	private ProjectDependencies computeProjectDependencies(Set<IInstallableUnit> projectUnits,
			IQueryable<IInstallableUnit> avaiableIUs) throws CoreException {
		if (projectUnits.isEmpty()) {
			return EMPTY_DEPENDENCIES;
		}
		Map<IRequirement, Collection<IInstallableUnit>> dependencies = slicer.computeDirectDependencies(projectUnits,
				avaiableIUs);
		return new ProjectDependencies(dependencies, projectUnits);
	}

	@Override
	public Optional<MavenProject> getProject(IInstallableUnit installableUnit) {
		return Optional.ofNullable(iuProjectMap.get(installableUnit));
	}

	@Override
	public ProjectDependencies getProjectDependecies(MavenProject mavenProject) {
		return projectDependenciesMap.getOrDefault(mavenProject, EMPTY_DEPENDENCIES);
	}

	@Override
	public Stream<Entry<MavenProject, Collection<IInstallableUnit>>> dependencies(
			Function<MavenProject, Collection<IInstallableUnit>> contextIuSupplier) {
		return projectDependenciesMap.entrySet().stream().map(pd -> new SimpleEntry<>(pd.getKey(),
				pd.getValue().getDependencies(contextIuSupplier.apply(pd.getKey()))));
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

	private static boolean isFragment(IInstallableUnit installableUnit) {
		return getFragmentCapability(installableUnit).findAny().isPresent();
	}

	private static Stream<IProvidedCapability> getFragmentCapability(IInstallableUnit installableUnit) {

		return installableUnit.getProvidedCapabilities().stream()
				.filter(cap -> BundlesAction.CAPABILITY_NS_OSGI_FRAGMENT.equals(cap.getNamespace()));
	}

}
