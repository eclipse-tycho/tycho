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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.core.bnd.BndPluginManager;

import aQute.bnd.build.Project;
import aQute.bnd.build.SubProject;
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
@Component(role = AbstractMavenLifecycleParticipant.class)
public class BndMavenLifecycleParticipant extends AbstractMavenLifecycleParticipant {

	private static final Set<Entry<String, String>> BND_TO_MAVEN_MAPPING = Map.of(//
			Constants.DEPENDSON, Artifact.SCOPE_RUNTIME, //
			Constants.BUILDPATH, Artifact.SCOPE_COMPILE, //
			Constants.TESTPATH, Artifact.SCOPE_TEST //
	).entrySet();

	@Requirement
	private Logger logger;

	@Requirement
	private BndPluginManager bndPluginManager;

	@Override
	public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
		Map<MavenProject, Project> bndProjects = getProjects(session);
		Map<String, BndMavenProject> manifestFirstProjects = getManifestFirstProjects(session, bndProjects.keySet());
		Map<String, BndMavenProject> bndWorkspaceProjects = new HashMap<>();
		for (Entry<MavenProject, Project> entry : bndProjects.entrySet()) {
			MavenProject mavenProject = entry.getKey();
			Project project = entry.getValue();
			logger.debug("==" + mavenProject.getId() + "==");
			List<SubProject> subProjects = project.getSubProjects();
			logger.debug("Main: " + project.getName());
			if (subProjects.isEmpty()) {
				bndWorkspaceProjects.put(project.getName(), new BndMavenProject(mavenProject, project, null));
			} else {
				for (SubProject subProject : subProjects) {
					logger.debug("Sub: " + subProject.getName());
					bndWorkspaceProjects.put(project.getName() + "." + subProject.getName(),
							new BndMavenProject(mavenProject, project, subProject.getName()));
				}
			}
		}
		Map<MavenProject, Set<String>> dependencyMap = new HashMap<>();
		for (Entry<MavenProject, Project> entry : bndProjects.entrySet()) {
			MavenProject mavenProject = entry.getKey();
			Set<String> added = getProjectSet(mavenProject, dependencyMap);
			Project bndProject = entry.getValue();
			try {
				for (Entry<String, String> mapping : BND_TO_MAVEN_MAPPING) {
					Set<String> requirements = bndProject.getMergedParameters(mapping.getKey()).keySet();
					String mavenScope = mapping.getValue();
					for (String required : requirements) {
						BndMavenProject bndMavenProject = bndWorkspaceProjects.get(required);
						if (bndMavenProject == null) {
							bndMavenProject = manifestFirstProjects.get(required);
						}
						if (bndMavenProject == null || bndMavenProject.mavenProject() == mavenProject) {
							continue;
						}
						MavenProject requiredMavenProject = bndMavenProject.mavenProject();
						logger.debug(mavenProject.getId() + " depends on reactor project "
								+ requiredMavenProject.getId() + " ...");
						Dependency dependency = new Dependency();
						dependency.setGroupId(requiredMavenProject.getGroupId());
						dependency.setArtifactId(requiredMavenProject.getArtifactId());
						dependency.setVersion(requiredMavenProject.getVersion());
						dependency.setScope(mavenScope);
						if (bndMavenProject.classifier() != null) {
							Dependency clone = dependency.clone();
							clone.setClassifier(bndMavenProject.classifier());
							clone.setType("jar");
							addDependency(mavenProject, clone, added);
						}
						dependency.setType(requiredMavenProject.getPackaging());
						addDependency(mavenProject, dependency, added);
					}
				}
			} catch (Exception e) {
				logError("Can't get dependents of project " + mavenProject.getId(), e);
			}
		}
		for (BndMavenProject bndMavenProject : manifestFirstProjects.values()) {
			MavenProject mavenProject = bndMavenProject.mavenProject();
			Set<String> added = getProjectSet(mavenProject, dependencyMap);
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
						BndMavenProject requiredMavenProject = bndWorkspaceProjects.get(bundle.trim());
						if (requiredMavenProject == null || requiredMavenProject.mavenProject() == mavenProject) {
							continue;
						}
						logger.debug(mavenProject.getId() + " depends on reactor project "
								+ requiredMavenProject.mavenProject().getId() + " ...");
						Dependency dependency = new Dependency();
						dependency.setGroupId(requiredMavenProject.mavenProject().getGroupId());
						dependency.setArtifactId(requiredMavenProject.mavenProject().getArtifactId());
						dependency.setVersion(requiredMavenProject.mavenProject().getVersion());
						addDependency(mavenProject, dependency, added);
					}
				}
			} catch (Exception e) {
				logError("Can't get dependents of project " + mavenProject.getId(), e);
			}

		}
	}

	private Map<String, BndMavenProject> getManifestFirstProjects(MavenSession session, Set<MavenProject> existing) {
		Map<String, BndMavenProject> result = new HashMap<>();
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
						result.put(bsn, new BndMavenProject(mavenProject, null, null));
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

	private static Set<String> getProjectSet(MavenProject mavenProject, Map<MavenProject, Set<String>> dependencyMap) {
		Set<String> set = dependencyMap.computeIfAbsent(mavenProject, nil -> new HashSet<>());
		mavenProject.getDependencies().stream().map(d -> getKey(d)).forEach(set::add);
		return set;
	}

	private static void addDependency(MavenProject mavenProject, Dependency dependency, Set<String> added) {
		if (added.add(getKey(dependency))) {
			mavenProject.getDependencies().add(dependency);
		}
	}

	private static String getKey(Dependency dependency) {
		return dependency.getManagementKey() + ":" + dependency.getVersion() + ":"
				+ Objects.requireNonNullElse(dependency.getClassifier(), "");
	}

}
