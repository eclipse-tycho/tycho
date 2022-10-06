/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;

/**
 * This mojo generates <a href="http://docs.osgi.org/specification/osgi.cmpn/8.0.0/service.component.html#service.component-component.description">
 * OSGi Declarative Services component description XMLs</a> based on
 * <a href="https://docs.osgi.org/javadoc/osgi.cmpn/8.0.0/org/osgi/service/component/annotations/package-summary.html">OSGi DS annotations</a>
 * in the {@code process-classes} phase.
 * The generated component description XMLs end up in {@code project.build.outputDirectory} below the given {@link DeclarativeServicesMojo#path}.
 * This mojo uses <a href="https://bnd.bndtools.org/">Bnd</a> under the hood.
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
	 * Controls the declarative services specification version to use as maximum.
	 * This mojo may generate component descriptions in a version lower than the given one in case the annotations don't require features from newer versions.
	 * Values need to be given in format {@code V<major>_<minor>} or {@code <major>.<minor>}.
	 */
	@Parameter(property = "tycho.ds.version", defaultValue = DeclarativeServiceConfigurationReader.DEFAULT_DS_VERSION)
	private String dsVersion = DeclarativeServiceConfigurationReader.DEFAULT_DS_VERSION;

	/**
	 * Enables the processing of declarative services by Tycho, this could be
	 * overridden by project specific configuration.
	 * If set to {@code true} will enable DS it for all projects except for those that have explicitly disabled
	 * <a href="https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.pde.doc.user%2Ftips%2Fpde_tips.htm&cp%3D4_4">DS processing in their per-project configuration</a>,
	 * if set to {@code false} will only process projects which have DS processing explicitly enabled in their per-project configuration.
	 */
	@Parameter(property = "tycho.ds.enabled", defaultValue = "false")
	private boolean enabled = false;

	/**
	 * Skips the generation of any DS processing regardless of project configuration
	 */
	@Parameter(property = "tycho.ds.skip", defaultValue = "false")
	private boolean skip = false;

	/**
	 * The desired path where to place component definitions. If it is given as relative path it is relative to {@code project.build.outputDirectory}.
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
		if (projectType instanceof OsgiBundleProject bundleProject) {
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
					List<ClasspathEntry> classpath = bundleProject.getClasspath(DefaultReactorProject.adapt(project));
					for (ClasspathEntry entry : classpath) {
						List<File> locations = entry.getLocations();
						for (File file : locations) {
							if (file.exists()) {
								analyzer.addClasspath(file);
							}
						}
					}
					// https://bnd.bndtools.org/instructions/dsannotations-options.html
					analyzer.setProperty(Constants.DSANNOTATIONS_OPTIONS, "version;maximum=" + configuration.getSpecificationVersion().toString());
					analyzer.addBasicPlugin(new DSAnnotations());
					analyzer.analyze();
					for (String warning : analyzer.getWarnings()) {
						getLog().warn(warning);
					}
					for (String error : analyzer.getErrors()) {
						getLog().error(error);
					}
					if (!analyzer.getErrors().isEmpty()) {
						throw new MojoFailureException("Generation of ds components failed, see log for details");
					}
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
				if (e instanceof MojoFailureException mfe) {
					throw mfe;
				}
				throw new MojoFailureException("Generation of ds components failed: " + e.getMessage(), e);
			}
		}
	}

}
