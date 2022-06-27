/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.ds;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.artifacts.configuration.DeclarativeServiceConfigurationReader;
import org.eclipse.tycho.classpath.ClasspathEntry;
import org.eclipse.tycho.core.DeclarativeServicesConfiguration;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.OsgiBundleProject;

import aQute.bnd.component.DSAnnotations;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;

/**
 * This mojo could be added to a build if validation of the classpath is desired
 * before the compile-phase.
 */
@Mojo(name = "declarative-services", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class DeclarativeServicesMojo extends AbstractMojo {

	/**
	 * Controls if the DS components annotations are made available on the
	 * compile-classpath, this means no explicit import is required.
	 */
	@Parameter(property = "tycho.ds.classpath", defaultValue = DeclarativeServiceConfigurationReader.DEFAULT_ADD_TO_CLASSPATH)
	private boolean classpath = Boolean.parseBoolean(DeclarativeServiceConfigurationReader.DEFAULT_ADD_TO_CLASSPATH);
	/**
	 * Controls the declarative services specification version to use.
	 */
	@Parameter(property = "tycho.ds.version", defaultValue = DeclarativeServiceConfigurationReader.DEFAULT_DS_VERSION)
	private String dsVersion = DeclarativeServiceConfigurationReader.DEFAULT_DS_VERSION;

	/**
	 * Enables the processing of declarative services by Tycho, this could be
	 * overridden by project specific configuration
	 */
	@Parameter(property = "tycho.ds.enabled", defaultValue = "false")
	private boolean enabled = false;

	/**
	 * Skips the generation of any DS processing regardless of project configuration
	 */
	@Parameter(property = "tycho.ds.skip", defaultValue = "false")
	private boolean skip = false;

	/**
	 * The desired path where to place component definitions
	 */
	@Parameter(property = "tycho.ds.path", defaultValue = DeclarativeServiceConfigurationReader.DEFAULT_PATH)
	private String path = "OSGI-INF";

	@Parameter(property = "project", readonly = true)
	protected MavenProject project;

	@Component(role = TychoProject.class)
	private Map<String, TychoProject> projectTypes;

	@Component
	private DeclarativeServiceConfigurationReader configurationReader;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			return;
		}
		TychoProject projectType = projectTypes.get(project.getPackaging());
		if (projectType instanceof OsgiBundleProject) {
			try {
				DeclarativeServicesConfiguration configuration = configurationReader.getConfiguration(project);
				if (configuration == null) {
					// nothing to do
					return;
				}
				File outputDirectory = new File(project.getBuild().getOutputDirectory());
				String childPath = configuration.getPath();
				File targetDirectory = new File(outputDirectory, childPath);
				File projectBaseDir = new File(project.getBasedir(), childPath);
				try (Jar mavenProjectJar = new Jar(project.getName(), outputDirectory, null);
						Analyzer analyzer = new Analyzer(mavenProjectJar)) {
					Map<String, Resource> directory = analyzer.getJar().getDirectory("OSGI-INF");
					if (directory != null) {
						// clear any existing entries
						directory.clear();
					}
					OsgiBundleProject bundleProject = (OsgiBundleProject) projectType;
					List<ClasspathEntry> classpath = bundleProject.getClasspath(DefaultReactorProject.adapt(project));
					for (ClasspathEntry entry : classpath) {
						analyzer.addClasspath(entry.getLocations());
					}
					analyzer.addBasicPlugin(new DSAnnotations());
					analyzer.analyze();
					String components = analyzer.getProperty("Service-Component");
					if (components == null || components.isBlank()) {
						// nothing to do...
						return;
					}
					for (String component : components.split(",\\s*")) {
						String name = FilenameUtils.getName(component);
						if (new File(projectBaseDir, name).isFile()) {
							// this is an exiting component definition, we should not mess with that...
							continue;
						}
						Resource resource = analyzer.getJar().getResource(component);
						if (resource != null) {
							File file = new File(targetDirectory, name);
							file.getParentFile().mkdirs();
							resource.write(file);
						}
					}
				}
			} catch (Exception e) {
				throw new MojoFailureException("generation of ds components failed: " + e.getMessage(), e);
			}
		}
	}

}
