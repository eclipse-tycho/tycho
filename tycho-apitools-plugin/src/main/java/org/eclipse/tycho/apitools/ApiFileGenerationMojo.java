/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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
package org.eclipse.tycho.apitools;

import java.io.File;
import java.util.jar.JarFile;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.pde.api.tools.internal.APIFileGenerator;

/**
 * performs generation of PDE-API Tools description
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_CLASSES, threadSafe = true)
public class ApiFileGenerationMojo extends AbstractMojo {

	@Parameter(property = "project", readonly = true)
	protected MavenProject project;

	@Parameter(defaultValue = "${project.build.directory}")
	protected File targetFolder;

	@Parameter(defaultValue = "${project.build.outputDirectory}")
	protected File binaryLocations;

	@Parameter(defaultValue = "${project.basedir}")
	protected File projectLocation;

	@Parameter(defaultValue = "${project.artifactId}_${project.version}")
	protected String projectName;

	/**
	 * @Since 3.1.0
	 */
	@Parameter(defaultValue = "false")
	private boolean allowNonApiProject;

	/**
	 * @Since 3.1.0
	 */
	@Parameter
	protected String encoding;

	/**
	 * @Since 3.1.0
	 */
	@Parameter
	protected boolean debug;

	/**
	 * @Since 3.1.0
	 */
	@Parameter
	protected String extraManifests;

	/**
	 * @Since 3.1.0
	 */
	@Parameter
	protected String extraSourceLocations;

	@Parameter(defaultValue = "false", property = "tycho.apitools.generate.skip")
	private boolean skip;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (new File(project.getBasedir(), JarFile.MANIFEST_NAME).isFile()) {
			synchronized (ApiFileGenerationMojo.class) {
				// TODO check if the generator is thread safe, then we can remove this!
				APIFileGenerator generator = new APIFileGenerator();
				generator.projectName = projectName;
				generator.projectLocation = projectLocation.getAbsolutePath();
				generator.binaryLocations = binaryLocations.getAbsolutePath();
				generator.targetFolder = targetFolder.getAbsolutePath();
				generator.allowNonApiProject = allowNonApiProject;
				generator.encoding = encoding;
				generator.debug = debug;
				generator.manifests = extraManifests;
				generator.sourceLocations = extraSourceLocations;
				generator.generateAPIFile();
			}
		}
	}

}
