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
package org.eclipse.tycho.bnd.mojos;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.ClasspathEntry;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.bnd.MavenProjectJar;
import org.eclipse.tycho.classpath.ClasspathContributor;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.bnd.BndPluginManager;
import org.eclipse.tycho.core.osgitools.BundleClassPath;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.OsgiBundleProject;
import org.eclipse.tycho.helper.PluginRealmHelper;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;

/**
 * The mojos support generation of the manifest file like it is done in PDE if
 * you choose to automatically generate metadata.
 */
@Mojo(name = "process", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class GenerateManifestMojo extends AbstractMojo {

	private static final Predicate<Path> CLASS_FILTER = resource -> {
		if (Files.isRegularFile(resource)) {
			return resource.getFileName().endsWith(".class");
		}
		return true;
	};

	@Component
	private BndPluginManager bndPluginManager;

	@Parameter(property = "project", readonly = true)
	protected MavenProject mavenProject;

	@Parameter(property = "session", readonly = true)
	protected MavenSession session;

	@Component
	private PluginRealmHelper pluginRealmHelper;
	@Component
	private TychoProjectManager projectManager;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		TychoProject tychoProject = projectManager.getTychoProject(mavenProject).orElse(null);
		if (tychoProject instanceof OsgiBundleProject osgi) {
			File basedir = mavenProject.getBasedir();
			File instructionsFile = new File(basedir, TychoConstants.PDE_BND);
			if (instructionsFile.isFile()) {
				try (Project project = new Project(getWorkspace(), basedir, instructionsFile);
						ProjectBuilder builder = new ProjectBuilder(project) {
							@Override
							public Jar getJarFromName(String name, String from) {
								Matcher m = TychoConstants.PLATFORM_URL_PATTERN.matcher(name);
								if (m.matches()) {
									TargetPlatform targetPlatform = projectManager.getTargetPlatform(mavenProject)
											.orElse(null);
									if (targetPlatform == null) {
										return null;
									}
									String pluginId = m.group(2);
									try {
										ArtifactKey artifact = targetPlatform
												.resolveArtifact(ArtifactType.TYPE_ECLIPSE_PLUGIN, pluginId, null);
										File artifactLocation = targetPlatform.getArtifactLocation(artifact);
										if (artifactLocation == null) {
											return null;
										}
										return new Jar(artifactLocation);
									} catch (Exception e) {
										return null;
									}
								}
								return super.getJarFromName(name, from);
							}
						};
						Jar jar = new MavenProjectJar(mavenProject, CLASS_FILTER)) {
					setupProject(project);
					BundleClassPath bundleClassPath = osgi
							.getBundleClassPath(DefaultReactorProject.adapt(mavenProject));
					builder.setBase(project.getBase());
					builder.setJar(jar);
					for (ClasspathEntry cpe : bundleClassPath.getClasspathEntries()) {
						cpe.getLocations().forEach(project::addClasspath);
					}
					try {
						pluginRealmHelper.visitPluginExtensions(mavenProject, session, ClasspathContributor.class,
								cpc -> {
									List<ClasspathEntry> list = cpc.getAdditionalClasspathEntries(mavenProject,
											Artifact.SCOPE_COMPILE);
									if (list != null && !list.isEmpty()) {
										for (ClasspathEntry entry : list) {
											try {
												builder.addClasspath(entry.getLocations());
											} catch (IOException e) {
												e.printStackTrace();
											}
										}
									}
								});
					} catch (Exception e) {
						throw new MojoExecutionException("can't call classpath contributors", e);
					}
					builder.build();
				} catch (MojoExecutionException e) {
					throw e;
				} catch (Exception e) {
					throw new MojoFailureException("Can't generate manifest data", e);
				}
			}
		}
	}

	void setupProject(Project bnd) throws Exception {
		bnd.setBase(mavenProject.getBasedir());
		bnd.setProperty(Constants.DEFAULT_PROP_SRC_DIR,
				mavenProject.getCompileSourceRoots().stream().collect(Collectors.joining(",")));
		bnd.setProperty(Constants.DEFAULT_PROP_BIN_DIR, mavenProject.getBuild().getOutputDirectory());
		bnd.setProperty(Constants.DEFAULT_PROP_TARGET_DIR, mavenProject.getBuild().getDirectory());
	}

	Workspace getWorkspace() throws Exception {
		Processor run = new Processor();
		run.setProperty(Constants.STANDALONE, "true");
		Workspace workspace = Workspace.createStandaloneWorkspace(run,
				new File(mavenProject.getBuild().getDirectory(), Project.BNDCNF).toURI());
		bndPluginManager.setupWorkspace(workspace);
		return workspace;
	}

}
