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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.pde.api.tools.internal.APIFileGenerator;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.model.project.EclipseProject;

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

	@Parameter(defaultValue = "false", property = "tycho.apitools.generate.skip")
	private boolean skip;

	/**
	 * If set to
	 * <code>true</true> all configured source folders in <code>build.properties</code>
	 * will be added as {@link #extraSourceLocations}
	 */
	@Parameter(defaultValue = "false")
	private boolean addSourceFolders;

	@Inject
	private TychoProjectManager projectManager;

	@Inject
	private BuildPropertiesParser buildPropertiesParser;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		Optional<EclipseProject> eclipseProject = projectManager.getEclipseProject(project);
		if (eclipseProject.isEmpty()
				|| !eclipseProject.get().hasNature("org.eclipse.pde.api.tools.apiAnalysisNature")) {
			return;
		}
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
				if (addSourceFolders) {
					List<File> list = new ArrayList<>(extraSourceLocations);
					for (Map.Entry<String, List<String>> entry : buildPropertiesParser
							.parse(DefaultReactorProject.adapt(project)).getJarToSourceFolderMap().entrySet()) {
						for (String sourceFolder : entry.getValue()) {
							list.add(canonicalFile(new File(project.getBasedir(), sourceFolder)));
						}
					}
					generator.sourceLocations = join(list);
				} else {
					generator.sourceLocations = join(extraSourceLocations);
				}
				generator.generateAPIFile();
			}
		}
	}

	private static File canonicalFile(File file) {
		try {
			return file.getCanonicalFile();
		} catch (IOException e) {
		}
		return file;
	}

	private static String join(List<File> list) {
		return list.isEmpty() ? null // join the elements so that the APIFileGenerator splits it correspondingly
				: list.stream().map(File::toString).collect(Collectors.joining(File.pathSeparator));
	}

}
