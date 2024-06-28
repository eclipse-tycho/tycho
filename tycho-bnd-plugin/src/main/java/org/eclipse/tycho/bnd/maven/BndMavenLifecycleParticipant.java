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
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.core.bnd.BndPluginManager;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;

/**
 * This component injects information from the BND model into the maven model,
 * currently the following actions are performed:
 * <ul>
 * <li><code>-dependson</code> for any reactor project is mapped to a
 * <code>runtime<code> maven dependency</li>
 * <li><code>-buildpath</code> for any reactor project is mapped to a
 * <code>compile<code> maven dependency</li>
 * <li><code>-testpath</code> for any reactor project is mapped to a
 * <code>test<code> maven dependency</li>
 * </ul>
 */
@Named
@Singleton
public class BndMavenLifecycleParticipant extends AbstractMavenLifecycleParticipant {

	private static final Set<Entry<String, String>> BND_TO_MAVEN_MAPPING = Map.of(//
			Constants.DEPENDSON, Artifact.SCOPE_RUNTIME, //
			Constants.BUILDPATH, Artifact.SCOPE_COMPILE, //
			Constants.TESTPATH, Artifact.SCOPE_TEST //
	).entrySet();

	@Inject
	private Logger logger;

	@Inject
	private BndPluginManager bndPluginManager;

	@Override
	public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
		Map<MavenProject, Project> bndProjects = getProjects(session);
		Map<String, MavenProject> manifestFirstProjects = getManifestFirstProjects(session, bndProjects.keySet());
		Map<String, MavenProject> bndWorkspaceProjects = bndProjects.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getValue().getName(), Entry<MavenProject, Project>::getKey, (a, b) -> {
					logger.warn(
							"Your reactor build contains duplicate BND projects from different workspace, build order might be insufficient!");
					logger.warn("\tProject 1 (selected): " + a.getBasedir());
					logger.warn("\tProject 2  (ignored): " + b.getBasedir());
					return a;
				}));
		for (Entry<MavenProject, Project> entry : bndProjects.entrySet()) {
			MavenProject mavenProject = entry.getKey();
			Project bndProject = entry.getValue();
			try {

				for (Entry<String, String> mapping : BND_TO_MAVEN_MAPPING) {
					Set<String> requirements = bndProject.getMergedParameters(mapping.getKey()).keySet();
					String mavenScope = mapping.getValue();
					for (String required : requirements) {
						MavenProject requiredMavenProject = bndWorkspaceProjects.get(required);
						if (requiredMavenProject == null) {
							requiredMavenProject = manifestFirstProjects.get(required);
						}
						if (requiredMavenProject == null || requiredMavenProject == mavenProject) {
							continue;
						}
						logger.debug(mavenProject.getId() + " depends on reactor project "
								+ requiredMavenProject.getId() + " ...");
						Dependency dependency = new Dependency();
						dependency.setGroupId(requiredMavenProject.getGroupId());
						dependency.setArtifactId(requiredMavenProject.getArtifactId());
						dependency.setVersion(requiredMavenProject.getVersion());
						dependency.setScope(mavenScope);
						mavenProject.getDependencies().add(dependency);
					}
				}
			} catch (Exception e) {
				logError("Can't get dependents of project " + mavenProject.getId(), e);
			}
		}
		for (MavenProject mavenProject : manifestFirstProjects.values()) {
			try {
				File file = new File(mavenProject.getBasedir(), "build.properties");
				if (file.isFile()) {
					Properties properties = new Properties();
					try (FileInputStream stream = new FileInputStream(file)) {
						properties.load(stream);
					}
					String property = properties.getProperty("additional.bundles");
					if (property == null || property.isBlank()) {
						continue;
					}
					for (String bundle : property.split(",")) {
						MavenProject requiredMavenProject = bndWorkspaceProjects.get(bundle.trim());
						if (requiredMavenProject == null || requiredMavenProject == mavenProject) {
							continue;
						}
						logger.debug(mavenProject.getId() + " depends on reactor project "
								+ requiredMavenProject.getId() + " ...");
						Dependency dependency = new Dependency();
						dependency.setGroupId(requiredMavenProject.getGroupId());
						dependency.setArtifactId(requiredMavenProject.getArtifactId());
						dependency.setVersion(requiredMavenProject.getVersion());
						mavenProject.getDependencies().add(dependency);
					}
				}
			} catch (Exception e) {
				logError("Can't get dependents of project " + mavenProject.getId(), e);
			}

		}
	}

	private Map<String, MavenProject> getManifestFirstProjects(MavenSession session, Set<MavenProject> existing) {
		Map<String, MavenProject> result = new HashMap<String, MavenProject>();
		for (MavenProject mavenProject : session.getProjects()) {
			if (existing.contains(mavenProject)) {
				continue;
			}
			File basedir = mavenProject.getBasedir();
			File manifestFile = new File(basedir, JarFile.MANIFEST_NAME);
			if (manifestFile.exists()) {
				try (FileInputStream stream = new FileInputStream(manifestFile)) {
					Manifest manifest = new Manifest(stream);
					String value = manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
					if (value != null) {
						String bsn = value.split(";", 2)[0];
						result.put(bsn, mavenProject);
					}
				} catch (Exception e) {
					logError("Can't read project " + mavenProject.getId(), e);
				}
			}
		}
		return result;
	}

	private Map<MavenProject, Project> getProjects(MavenSession session) {
		Set<Workspace> workspaces = new HashSet<>();
		HashMap<MavenProject, Project> result = new HashMap<>();
		for (MavenProject mavenProject : session.getProjects()) {
			if (isBNDProject(mavenProject)) {
				try {
					File basedir = mavenProject.getBasedir();
					Workspace ws = Workspace.findWorkspace(basedir.getParentFile());
					if (workspaces.add(ws)) {
						bndPluginManager.setupWorkspace(ws);
					}
					Project project = ws.getProject(basedir.getName());
					mavenProject.setContextValue(Project.class.getName(), project);
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

	private void logError(String msg, Exception e) {
		if (logger.isDebugEnabled()) {
			logger.error(msg, e);
		} else {
			logger.warn(msg + ": " + e);
		}
	}

}
