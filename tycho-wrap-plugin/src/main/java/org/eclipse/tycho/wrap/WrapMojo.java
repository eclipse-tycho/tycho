/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 ******************************************************************************/
package org.eclipse.tycho.wrap;

import java.io.File;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.settings.Settings;
import org.osgi.framework.Constants;

import aQute.bnd.build.Project;
import aQute.bnd.maven.lib.configuration.BndConfiguration;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;
import aQute.bnd.print.JarPrinter;
import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;

/**
 * This mojos allows creating OSGi jars by specifying an arbitrary input and output, 
 * some <a href="https://bnd.bndtools.org/chapters/160-jars.html">bnd instructions</a> 
 * and (optionally) attach the result to the maven project.
 * 
 * This has the advantage that projects are able to publish 
 * two "flavors" of their artifact: a plain one and an OSGi-fied one 
 * that could help to convince projects to provide such things as 
 * it has zero influence to their build and ways how they build artifacts.
 * 
 */
@Mojo(name = "wrap", requiresProject = true, threadSafe = true, defaultPhase = LifecyclePhase.PACKAGE)
public class WrapMojo extends AbstractMojo {

	private static final String[] HEADERS = { Constants.BUNDLE_SYMBOLICNAME, Constants.BUNDLE_VERSION };

	@Component
	private MavenProject project;

	@Parameter(defaultValue = "${settings}", readonly = true)
	Settings settings;

	@Component
	private MojoExecution mojoExecution;

	@Component
	private MavenProjectHelper helper;

	/**
	 * File path to a bnd file containing bnd instructions for this project.
	 * Defaults to {@code bnd.bnd}. The file path can be an absolute or relative to
	 * the project directory.
	 * <p>
	 * The bnd instructions for this project are merged with the bnd instructions,
	 * if any, for the parent project.
	 */
	// This is not used and is for doc only; see
	// BndConfiguration#loadProperties and
	// AbstractBndMavenPlugin for reference
	@Parameter(defaultValue = Project.BNDFILE)
	String bndfile;

	/**
	 * Bnd instructions for this project specified directly in the pom file. This is
	 * generally be done using a {@code <![CDATA[]]>} section. If the project has a
	 * {@link #bndfile}, then this configuration element is ignored.
	 * <p>
	 * The bnd instructions for this project are merged with the bnd instructions,
	 * if any, for the parent project.
	 */
	// This is not used and is for doc only; see
	// BndConfiguration#loadProperties and
	// AbstractBndMavenPlugin for reference
	@Parameter
	String bnd;

	@Parameter(required = true, property = "input", defaultValue = "${project.build.directory}/${project.build.finalName}.${project.packaging}")
	private File input;

	@Parameter(required = true, property = "output", defaultValue = "${project.build.directory}/${project.build.finalName}-bundle.${project.packaging}")
	private File output;

	/**
	 * If enabled attach the generated file as an artifact to the project
	 */
	@Parameter(required = false, defaultValue = "true", property = "attach")
	private boolean attach;

	/**
	 * The classifier to use when attach this to the project
	 */
	@Parameter(defaultValue = "bundle", property = "classifier")
	private String classifier;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		BndConfiguration configuration = new BndConfiguration(project, mojoExecution);

		try (Jar jar = new Jar(output.getName(), input, Pattern.compile(JarFile.MANIFEST_NAME));
				Analyzer analyzer = new Analyzer(jar)) {
			configuration.loadProperties(analyzer);
			if (analyzer.getProperty(Constants.BUNDLE_VERSION) == null) {
				Version version = new MavenVersion(project.getVersion()).getOSGiVersion();
				analyzer.setProperty(Constants.BUNDLE_VERSION, version.toString());
			}
			if (analyzer.getProperty(Constants.BUNDLE_SYMBOLICNAME) == null) {
				analyzer.setProperty(Constants.BUNDLE_SYMBOLICNAME, project.getArtifactId());
			}
			if (analyzer.getProperty(Constants.BUNDLE_NAME) == null) {
				analyzer.setProperty(Constants.BUNDLE_NAME, project.getName());
			}
			Set<Artifact> artifacts = project.getArtifacts();
			for (Artifact artifact : artifacts) {
				File cpe = artifact.getFile();
				try {
					analyzer.addClasspath(cpe);
				} catch (Exception e) {
					// just go on... it might be not a jar or something else not usable
				}
			}
			Manifest manifest = analyzer.calcManifest();
			jar.setManifest(manifest);
			jar.write(output);
			analyzer.getWarnings().forEach(getLog()::warn);
			analyzer.getErrors().forEach(getLog()::error);
			Attributes mainAttributes = manifest.getMainAttributes();
			for (String header : HEADERS) {
				getLog().info(header + ": " + mainAttributes.getValue(header));
			}
			try (JarPrinter jarPrinter = new JarPrinter()) {
				jarPrinter.doPrint(jar, JarPrinter.IMPEXP, false, false);
				getLog().info(jarPrinter.toString());
			}
		} catch (MojoFailureException e) {
			throw e;
		} catch (MojoExecutionException e) {
			throw e;
		} catch (Exception e) {
			throw new MojoFailureException("wrapping input " + input + " failed: " + e, e);
		}
		if (attach) {
			helper.attachArtifact(project, output, classifier);
		}
	}

}
