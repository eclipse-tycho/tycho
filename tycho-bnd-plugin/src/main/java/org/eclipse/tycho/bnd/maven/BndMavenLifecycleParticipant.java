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
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.inject.Inject;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.bnd.mojos.BndRunMojo;
import org.eclipse.tycho.bndlib.BndRunFile;
import org.eclipse.tycho.core.bnd.BndPluginManager;

import aQute.bnd.build.Project;
import aQute.bnd.build.SubProject;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import biz.aQute.resolve.Bndrun;

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
			Constants.RUNBUNDLES, Artifact.SCOPE_RUNTIME, //
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
		Workspace.setDriver(TychoConstants.DRIVER_NAME);
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
					Set<String> requirements = getRequirements(bndProject, mavenProject, session, mapping.getKey());
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

	private Set<String> getRequirements(Project bndProject, MavenProject mavenProject, MavenSession session,
			String property) throws MavenExecutionException {
		if (Constants.RUNBUNDLES.equals(property)) {
			// run bundles are from bndruns and must be handled different
			Plugin plugin = mavenProject.getPlugin(Plugin.constructKey("org.eclipse.tycho", "tycho-bnd-plugin"));
			Set<String> selectedBndRuns = new HashSet<>();
			if (plugin != null) {
				for (PluginExecution pluginExecution : plugin.getExecutions()) {
					if (pluginExecution.getGoals().contains(BndRunMojo.NAME)) {
						String exportsProperty = session.getUserProperties()
								.getProperty(BndRunMojo.BNDRUN_EXPORTS_PROPERTY);
						if (exportsProperty == null) {
							Object configuration = pluginExecution.getConfiguration();
							if (configuration instanceof Xpp3Dom dom) {
								XmlPlexusConfiguration cfg = new XmlPlexusConfiguration(dom);
								PlexusConfiguration child = cfg.getChild(BndRunMojo.BNDRUN_EXPORTS_NAME, false);
								if (child != null) {
									PlexusConfiguration[] children = child.getChildren();
									for (PlexusConfiguration c : children) {
										selectedBndRuns.add(c.getValue());
									}
								}
							}
						} else {
							for (String run : exportsProperty.split(",")) {
								selectedBndRuns.add(run);
							}
						}
					}
				}
			}
			if (selectedBndRuns.isEmpty()) {
				return Set.of();
			}
			try {
				List<BndRunFile> bndRuns = BndRunMojo.getBndRuns(mavenProject.getBasedir().toPath(), selectedBndRuns);
				if (bndRuns.isEmpty()) {
					return Set.of();
				}
				Set<String> dependencies = new HashSet<>();
				for (BndRunFile runFile : bndRuns) {
					try {
						Bndrun bndrun = Bndrun.createBndrun(bndProject.getWorkspace(), runFile.path().toFile());
						dependencies.addAll(bndrun.getMergedParameters(property).keySet());
					} catch (Exception e) {
						throw new MavenExecutionException("can't read required bnd run " + runFile.path(), e);
					}
				}
				return dependencies;
			} catch (MojoExecutionException e) {
				throw new MavenExecutionException("can't read required bnd runs", e.getCause());
			}
		}
		return bndProject.getMergedParameters(property).keySet();
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
