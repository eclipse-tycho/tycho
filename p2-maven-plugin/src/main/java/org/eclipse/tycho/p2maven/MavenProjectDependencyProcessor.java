/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;

/**
 * THis component computes dependencies between projects
 *
 */
@Named
@Singleton
public class MavenProjectDependencyProcessor {

	@Inject
	private InstallableUnitGenerator generator;

	/**
	 * Computes the {@link ProjectDependencyClosure} of the given collection of
	 * projects.
	 * 
	 * @param projects                  the projects to include in the closure
	 * @param session                   the maven session for this request
	 * @param profilePropertiesSupplier supplier of context IUs for a project that
	 *                                  represent the the profile properties to
	 *                                  consider during resolution, can be empty in
	 *                                  which case a filter is always considered a
	 *                                  match
	 * @return the computed {@link ProjectDependencyClosure}
	 * @throws CoreException if computation failed
	 */
	public ProjectDependencyClosure computeProjectDependencyClosure(Collection<MavenProject> projects,
			MavenSession session)
			throws CoreException {
		Objects.requireNonNull(session);
		Map<MavenProject, Collection<IInstallableUnit>> projectIUMap = generator.getInstallableUnits(projects, session);
		return new ProjectDependencyClosureGraph(projectIUMap);
	}

	private static boolean isMatch(IRequirement requirement, Collection<IInstallableUnit> contextIUs) {
		IMatchExpression<IInstallableUnit> filter = requirement.getFilter();
		if (filter == null || contextIUs.isEmpty()) {
			return true;
		}
		return contextIUs.stream().anyMatch(contextIU -> filter.isMatch(contextIU));
	}

	public static final class ProjectDependencies {

		private final Map<IRequirement, Collection<IInstallableUnit>> requirementsMap;
		final Set<IInstallableUnit> projectUnits;

		ProjectDependencies(Map<IRequirement, Collection<IInstallableUnit>> requirementsMap,
				Set<IInstallableUnit> projectUnits) {
			this.requirementsMap = requirementsMap;
			this.projectUnits = projectUnits;
		}

		public Collection<IInstallableUnit> getDependencies(Collection<IInstallableUnit> contextIUs) {
			return requirementsMap.entrySet().stream().filter(entry -> isMatch(entry.getKey(), contextIUs))
					.flatMap(entry -> entry.getValue().stream().filter(unit -> !projectUnits.contains(unit))
							.limit(entry.getKey().getMax()))
					.distinct().toList();
		}


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
		ProjectDependencies getProjectDependecies(MavenProject mavenProject);

		Collection<IInstallableUnit> getProjectUnits(MavenProject mavenProject);

		/**
		 * @return a stream of all contained maven projects with dependecies
		 */
		Stream<Entry<MavenProject, Collection<IInstallableUnit>>> dependencies(
				Function<MavenProject, Collection<IInstallableUnit>> contextIuSupplier);

		/**
		 * Given a maven project returns all other maven projects this one (directly)
		 * depends on
		 * 
		 * @param mavenProject the maven project for which all direct dependent projects
		 *                     should be collected
		 * @param contextIUs   the context IUs to filter dependencies
		 * @return the collection of projects this maven project depend on in this
		 *         closure
		 */
		Collection<MavenProject> getDependencyProjects(MavenProject mavenProject,
				Collection<IInstallableUnit> contextIUs);

		/**
		 * Check if the given unit is a fragment
		 * 
		 * @param mavenProject the project to check
		 * @return <code>true</code> if this is a fragment, <code>false</code> otherwise
		 */
		boolean isFragment(MavenProject mavenProject);

	}
}
