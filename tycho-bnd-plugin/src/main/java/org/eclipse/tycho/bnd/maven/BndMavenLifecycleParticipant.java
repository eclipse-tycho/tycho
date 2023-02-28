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
package org.eclipse.tycho.bnd.maven;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.TychoConstants;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;

@Component(role = AbstractMavenLifecycleParticipant.class)
public class BndMavenLifecycleParticipant extends AbstractMavenLifecycleParticipant {

	@Requirement
	private Logger logger;

	@Override
	public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
		Map<MavenProject, Project> projects = getProjects(session);
		for (Project project : projects.values()) {
			try {
				project.prepare();
			} catch (Exception e) {
				logError("Can't prepare project " + project.getName(), e);
			}
		}
		for (Entry<MavenProject, Project> entry : projects.entrySet()) {
			MavenProject mavenProject = entry.getKey();
			Project project = entry.getValue();
			try {
				Set<Project> dependents = project.getBuildDependencies();
				for (Project depends : dependents) {
					logger.debug(project.getName() + " depends on " + depends.getName() + " ...");
					Dependency dependency = new Dependency();
					dependency.setGroupId(depends.getProperty(TychoConstants.PROP_GROUP_ID));
					dependency.setArtifactId(depends.getProperty(TychoConstants.PROP_ARTIFACT_ID));
					dependency.setVersion(depends.getProperty(TychoConstants.PROP_VERSION));
					mavenProject.getDependencies().add(dependency);
				}
			} catch (Exception e) {
				logError("Can't get dependents of project " + project.getName(), e);
			}
		}
	}

	private Map<MavenProject, Project> getProjects(MavenSession session) {
		HashMap<MavenProject, Project> result = new HashMap<MavenProject, Project>();
		for (MavenProject mavenProject : session.getProjects()) {
			if (isBNDProject(mavenProject)) {
				try {
					Project project = Workspace.getProject(mavenProject.getBasedir());
					setProperty(project, TychoConstants.PROP_GROUP_ID, mavenProject.getGroupId());
					setProperty(project, TychoConstants.PROP_ARTIFACT_ID, mavenProject.getArtifactId());
					setProperty(project, TychoConstants.PROP_VERSION, mavenProject.getVersion());
					result.put(mavenProject, project);
				} catch (Exception e) {
					logError("Can't read project " + mavenProject.getId(), e);
				}
			}
		}
		return result;
	}

	static boolean isBNDProject(MavenProject mavenProject) {
		if (mavenProject.getPlugin("org.eclipse.tycho:tycho-bnd-plugin") == null) {
			return false;
		}
		File basedir = mavenProject.getBasedir();
		File bndFile = new File(basedir, Project.BNDFILE);
		try {
			return bndFile.isFile() && Workspace.findWorkspace(basedir) != null;
		} catch (Exception e) {
			return false;
		}
	}

	private void setProperty(Project project, String key, String value) {
		String property = project.getProperty(key);
		if (property == null || property.isBlank()) {
			project.setProperty(key, value);
		}
	}

	private void logError(String msg, Exception e) {
		if (logger.isDebugEnabled()) {
			logger.error(msg, e);
		} else {
			logger.warn(msg + ": " + e);
		}
	}

}
