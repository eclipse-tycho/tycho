/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
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
package org.eclipse.tycho.repository.plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.helper.PluginConfigurationHelper;
import org.eclipse.tycho.helper.ProjectHelper;
import org.eclipse.tycho.packaging.RepositoryGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
public class TychoRepositoryPluginMavenLifecycleParticipant extends AbstractMavenLifecycleParticipant {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final Map<String, RepositoryGenerator> generators;
	private final PluginConfigurationHelper configurationHelper;
	private final ProjectHelper projectHelper;

	@Inject
	public TychoRepositoryPluginMavenLifecycleParticipant(Map<String, RepositoryGenerator> generators,
														  PluginConfigurationHelper configurationHelper,
														  ProjectHelper projectHelper) {
		this.generators = generators;
		this.configurationHelper = configurationHelper;
		this.projectHelper = projectHelper;
	}

	@Override
	public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
		List<MavenProject> projects = session.getProjects();
		for (MavenProject project : projects) {
			Plugin plugin = project.getPlugin("org.eclipse.tycho:tycho-repository-plugin");
			if ("repository".equals(project.getPackaging()) && plugin != null) {
				Set<MavenProject> added = new HashSet<MavenProject>();
				for (PluginExecution execution : plugin.getExecutions()) {
					for (String goal : execution.getGoals()) {
						addInterestingProjects(project, projects, session, goal, added);
					}
				}
			}
		}
	}

	private void addInterestingProjects(MavenProject project, List<MavenProject> projects, MavenSession session,
			String goal, Set<MavenProject> added) {
		Xpp3Dom configuration = projectHelper.getPluginConfiguration("org.eclipse.tycho", "tycho-repository-plugin",
				goal, project, session);
		String repoType = configurationHelper.getConfiguration(configuration)
				.getString(PackageRepositoryMojo.PARAMETER_REPOSITORY_TYPE)
				.orElse(PackageRepositoryMojo.DEFAULT_REPOSITORY_TYPE);
		RepositoryGenerator generator = generators.get(repoType);
		if (generator == null) {
			logger.warn(
					"Can't determine projects that should be declared as automatic discovered dependencies because RepositoryGenerator of type '"
							+ repoType + "' was not found!");
			return;
		}
		for (MavenProject other : projects) {
			if (other == project || added.contains(other)) {
				continue;
			}
			if (generator.isInteresting(other)) {
				Dependency dependency = new Dependency();
				dependency.setGroupId(other.getGroupId());
				dependency.setArtifactId(other.getArtifactId());
				dependency.setVersion(other.getVersion());
				project.getModel().addDependency(dependency);
				added.add(other);
			}
		}

	}

}
