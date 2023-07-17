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

import java.util.List;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;

@Component(role = AbstractMavenLifecycleParticipant.class)
public class TychoRepositoryPluginMavenLifecycleParticipant extends AbstractMavenLifecycleParticipant {

	@Override
	public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
		List<MavenProject> projects = session.getProjects();
		for (MavenProject project : projects) {
			if ("repository".equals(project.getPackaging()) && project.getPlugin("org.eclipse.tycho:tycho-repository-plugin") != null) {
				addInterestingProjects(project, projects);
			}
		}
	}

	private void addInterestingProjects(MavenProject project, List<MavenProject> projects) {
		for (MavenProject other : projects) {
			if (other == project) {
				continue;
			}
			if (PackageRepositoryMojo.isInteresting(other)) {
				Dependency dependency = new Dependency();
				dependency.setGroupId(other.getGroupId());
				dependency.setArtifactId(other.getArtifactId());
				dependency.setVersion(other.getVersion());
				project.getModel().addDependency(dependency);
			}
		}
	}

}
