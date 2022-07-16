/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.query.CollectionResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.tycho.p2maven.io.MetadataIO;

/**
 * THis component computes dependencies between projects
 *
 */
@Component(role = MavenProjectDependencyProcessor.class)
public class MavenProjectDependencyProcessor {

	private static final boolean DUMP_DATA = Boolean.getBoolean("tycho.p2.dump")
			|| Boolean.getBoolean("tycho.p2.dump.dependencies");

	@Requirement
	private InstallableUnitGenerator generator;

	@Requirement
	private InstallableUnitSlicer slicer;

	/**
	 * Computes the {@link ProjectDependencyClosure} of the given collection of
	 * projects.
	 * 
	 * @param projects the projects to include in the closure
	 * @param session  the maven session for this request
	 * @return the computed {@link ProjectDependencyClosure}
	 * @throws CoreException if computation failed
	 */
	public ProjectDependencyClosure computeProjectDependencyClosure(Collection<MavenProject> projects,
			MavenSession session) throws CoreException {
		Objects.requireNonNull(session);
		Map<MavenProject, Collection<IInstallableUnit>> projectIUMap = generator.getInstallableUnits(projects, session);
		Collection<IInstallableUnit> availableIUs = projectIUMap.values().stream().flatMap(Collection::stream)
				.collect(Collectors.toSet());

		List<String> goals = session.getGoals();
		boolean onlyDirect = goals.size() == 1 && "dependency:tree".equals(goals.get(0));

		Map<MavenProject, Collection<IInstallableUnit>> projectDependenciesMap = computeProjectDependencies(projects,
				new CollectionResult<IInstallableUnit>(availableIUs), projectIUMap, onlyDirect);
		Map<IInstallableUnit, MavenProject> iuProjectMap = new HashMap<IInstallableUnit, MavenProject>();
		for (var entry : projectIUMap.entrySet()) {
			MavenProject mavenProject = entry.getKey();
			for (IInstallableUnit iu : entry.getValue()) {
				iuProjectMap.put(iu, mavenProject);
			}
		}
		return new ProjectDependencyClosure() {

			@Override
			public Optional<MavenProject> getProject(IInstallableUnit installableUnit) {
				return Optional.ofNullable(iuProjectMap.get(installableUnit));
			}

			@Override
			public Collection<IInstallableUnit> getProjectDependecies(MavenProject mavenProject) {
				return projectDependenciesMap.getOrDefault(mavenProject, Collections.emptyList());
			}

			@Override
			public Stream<Entry<MavenProject, Collection<IInstallableUnit>>> dependencies() {
				return projectDependenciesMap.entrySet().stream();
			}

			@Override
			public boolean isFragment(MavenProject mavenProject) {

				return getProjectUnits(mavenProject).stream().anyMatch(MavenProjectDependencyProcessor::isFragment);
			}

			@Override
			public Collection<IInstallableUnit> getProjectUnits(MavenProject mavenProject) {
				Collection<IInstallableUnit> collection = projectIUMap.get(mavenProject);
				if (collection != null) {
					return collection;
				}
				return Collections.emptyList();
			}

		};
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
	private Map<MavenProject, Collection<IInstallableUnit>> computeProjectDependencies(
			Collection<MavenProject> projects, IQueryable<IInstallableUnit> avaiableIUs,
			Map<MavenProject, Collection<IInstallableUnit>> projectIUMap, boolean direct) throws CoreException {
		List<CoreException> errors = new CopyOnWriteArrayList<CoreException>();
		Map<MavenProject, Collection<IInstallableUnit>> result = new ConcurrentHashMap<MavenProject, Collection<IInstallableUnit>>();
		projects.parallelStream().unordered().takeWhile(nil -> errors.isEmpty()).forEach(project -> {
			try {
				Collection<IInstallableUnit> projectDependencies = computeProjectDependencies(projectIUMap.get(project),
						avaiableIUs, direct);
				result.put(project, projectDependencies);
				if (DUMP_DATA) {
					File file = new File(project.getBasedir(), "project-dependencies.xml");
					try {
						new MetadataIO().writeXML(Collections.unmodifiableCollection(projectDependencies), file);
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
	private Collection<IInstallableUnit> computeProjectDependencies(Collection<IInstallableUnit> projectUnits,
			IQueryable<IInstallableUnit> avaiableIUs, boolean direct) throws CoreException {
		if (projectUnits.isEmpty()) {
			return Collections.emptyList();
		}
		Set<IInstallableUnit> resolved;
		if (direct) {
			resolved = slicer.computeDirectDependencies(projectUnits, avaiableIUs).toSet();
		} else {
			resolved = new LinkedHashSet<IInstallableUnit>(
					slicer.computeDependencies(projectUnits, avaiableIUs).toSet());
			resolved.removeAll(projectUnits);
		}
		// now we need to filter all fragments that we are a host!
		// for example SWT creates an explicit requirement to its fragments and we don't
		// want them here as we already have resolved them earlier!
		resolved.removeIf(iu -> hasAnyHost(iu, projectUnits));
		return resolved;
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

	private static boolean isFragment(IInstallableUnit installableUnit) {
		return getFragmentCapability(installableUnit).findAny().isPresent();
	}

	private static Stream<IProvidedCapability> getFragmentCapability(IInstallableUnit installableUnit) {

		return installableUnit.getProvidedCapabilities().stream().filter(cap -> {
			return BundlesAction.CAPABILITY_NS_OSGI_FRAGMENT.equals(cap.getNamespace());
		});
	}

	private static Stream<IRequirement> getFragmentHostRequirement(IInstallableUnit installableUnit) {
		return getFragmentCapability(installableUnit).map(provided -> {
			String hostName = provided.getName();
			for (IRequirement requirement : installableUnit.getRequirements()) {
				if (requirement instanceof IRequiredCapability) {
					IRequiredCapability requiredCapability = (IRequiredCapability) requirement;
					if (hostName.equals(requiredCapability.getName())) {
						return requirement;
					}
				}
			}
			return null;
		}).filter(Objects::nonNull);
	}

	public static interface ProjectDependencyClosure {

		/**
		 * 
		 * @param dependency
		 * @return the project that provides the given {@link IInstallableUnit} or an
		 *         empty Optional if no project in this closure provides this unit.
		 */
		Optional<MavenProject> getProject(IInstallableUnit dependency);

		/**
		 * @param mavenProject
		 * @return the dependencies of this project inside the closure
		 */
		Collection<IInstallableUnit> getProjectDependecies(MavenProject mavenProject);

		Collection<IInstallableUnit> getProjectUnits(MavenProject mavenProject);

		/**
		 * @return a stream of all contained maven projects with dependecies
		 */
		Stream<Entry<MavenProject, Collection<IInstallableUnit>>> dependencies();

		/**
		 * Given a maven project returns all other maven projects this one depends on
		 * 
		 * @param mavenProject
		 * @return the collection of projects this maven project depend on in this
		 *         closure
		 */
		default Collection<MavenProject> getDependencyProjects(MavenProject mavenProject) {
			return getProjectDependecies(mavenProject).stream().flatMap(dependency -> getProject(dependency).stream())
					.distinct().collect(Collectors.toList());
		}

		/**
		 * Check if the given unit is a fragment
		 * 
		 * @param installableUnit the unit to check
		 * @return <code>true</code> if this is a fragment, <code>false</code> otherwise
		 */
		boolean isFragment(MavenProject mavenProject);

	}
}
