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
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

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
	protected List<File> extraManifests = List.of();

	/**
	 * @Since 3.1.0
	 */
	@Parameter
	protected List<File> extraSourceLocations = List.of();

	/**
	 * @Since 3.1.0
	 */
	@Parameter(defaultValue = "eclipse-plugin-project")
	private Set<String> supportedPackagingTypes;

	@Parameter(defaultValue = "false", property = "tycho.apitools.generate.skip")
	private boolean skip;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (new File(project.getBasedir(), JarFile.MANIFEST_NAME).isFile()
				|| extraManifests.stream().anyMatch(File::isFile)) {
			synchronized (ApiFileGenerationMojo.class) {
				// TODO check if the generator is thread safe, then we can remove this!
				if (!binaryLocations.exists()) {
					binaryLocations.mkdirs();
				}
				APIFileGenerator generator = new APIFileGenerator();
				generator.projectName = projectName;
				generator.projectLocation = projectLocation.getAbsolutePath();
				generator.binaryLocations = binaryLocations.getAbsolutePath();
				generator.targetFolder = targetFolder.getAbsolutePath();
				generator.allowNonApiProject = allowNonApiProject;
				generator.encoding = encoding;
				generator.debug = debug;
				generator.manifests = join(extraManifests);
				generator.sourceLocations = join(extraSourceLocations);
				generator.generateAPIFile();
			}
		}
	}

	private String join(List<File> list) {
		return list.isEmpty() ? null // join the elements so that the APIFileGenerator splits it correspondingly
				: list.stream().map(File::toString).collect(Collectors.joining(File.pathSeparator));
	}

}
