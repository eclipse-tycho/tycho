/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
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

import javax.inject.Singleton;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "initialize", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class BndInitMojo extends AbstractBndMojo {

	/**
	 * The filename of the tycho generated POM file.
	 */
	@Parameter(defaultValue = ".tycho-consumer-pom.xml", property = "tycho.bnd.consumerpom.file")
	protected String tychoPomFilename;

	/**
	 * If deleteOnExit is true the file will be marked for deletion on JVM exit
	 */
	@Parameter(defaultValue = "true", property = "tycho.bnd.consumerpom.delete")
	protected boolean deleteOnExit = true;

	/**
	 * Indicate if the generated tycho POM should become the new project.
	 */
	@Parameter(defaultValue = "true", property = "tycho.bnd.consumerpom.update")
	protected boolean updatePomFile = true;

	@Parameter(defaultValue = "false", property = "tycho.bnd.consumerpom.skip")
	protected boolean skipPomGeneration;

	/**
	 * The directory where the tycho generated POM file will be written to.
	 */
	@Parameter(defaultValue = "${project.basedir}", property = "tycho.bnd.consumerpom.directory")
	protected File outputDirectory;

	@Singleton
	protected ModelWriter modelWriter;

	@Singleton
	protected ModelReader modelReader;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		fixupPolyglot();
		writeConsumerPom();
	}

	private void writeConsumerPom() throws MojoExecutionException {
		if (skipPomGeneration) {
			return;
		}
		Model projectModel;
		try {
			projectModel = modelReader.read(mavenProject.getFile(), Map.of());
		} catch (IOException e) {
			throw new MojoExecutionException("reading the model failed!", e);
		}
		projectModel.setBuild(null);
		projectModel.setVersion(mavenProject.getVersion());
		projectModel.setGroupId(mavenProject.getGroupId());
		projectModel.setParent(null);
		List<Dependency> dependencies = projectModel.getDependencies();
		dependencies.clear();
		dependencies.addAll(Objects.requireNonNullElse(mavenProject.getDependencies(), Collections.emptyList()));
		File output = new File(outputDirectory, tychoPomFilename);
		if (deleteOnExit) {
			output.deleteOnExit();
		}
		try {
			modelWriter.write(output, Map.of(), projectModel);
		} catch (IOException e) {
			throw new MojoExecutionException("writing the model failed!", e);
		}
		if (updatePomFile) {
			mavenProject.setFile(output);
		}
	}

	private void fixupPolyglot() {
		/*
		 * bnd instructions might include wildcards (e.g. -sub *.bnd) these commands get
		 * confused if the automatic polyglot file named after the bnd file
		 * (.polyglot.bnd.bnd) is present and goes wild. We simply rename it here to xml
		 * to avoid any confusion.
		 */
		File basedir = mavenProject.getBasedir();
		File[] files = basedir.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return pathname.getName().startsWith(".polyglot.") && pathname.getName().endsWith(".bnd");
			}

		});
		for (File file : files) {
			File moved = new File(file.getParentFile(), ".polyglot.xml");
			if (file.renameTo(moved)) {
				mavenProject.setFile(moved);
				moved.deleteOnExit();
			}
		}
	}

}
