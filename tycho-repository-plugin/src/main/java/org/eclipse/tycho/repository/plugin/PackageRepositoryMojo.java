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
package org.eclipse.tycho.repository.plugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.eclipse.tycho.packaging.RepositoryGenerator;

/**
 * Generates an OSGi repository from the current reactor projects
 *
 */
@Mojo(name = "package-repository")
public class PackageRepositoryMojo extends AbstractMojo {

	static final String DEFAULT_REPOSITORY_TYPE = MavenRepositoryGenerator.OSGI_REPOSITORY;

	static final String PARAMETER_REPOSITORY_TYPE = "repositoryType";

	@Parameter(property = "session", readonly = true)
	protected MavenSession session;

	@Parameter(defaultValue = "${project.build.directory}")
	private File destination;

	@Parameter(property = "project", readonly = true)
	private MavenProject project;

	/**
	 * Configures the used repository type
	 */
	@Parameter(defaultValue = DEFAULT_REPOSITORY_TYPE, name = PARAMETER_REPOSITORY_TYPE)
	private String repositoryType;

//	@Component
//	PluginRealmHelper pluginRealmHelper;

	// TODO actually we like to want use PluginRealmHelper but this currently do not
	// work due to how maven realms work
	@Component
	Map<String, RepositoryGenerator> repositoryGenerators;

	@Component(role = Archiver.class, hint = "zip")
	private ZipArchiver zipArchiver;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		CompletableFuture<File> repository = new CompletableFuture<File>();
		RepositoryGenerator generator = repositoryGenerators.get(repositoryType);
		if (generator != null) {
			List<MavenProject> projects = session.getProjects().stream().filter(generator::isInteresting)
					.filter(p -> p != project).toList();
			try {
				repository.complete(generator.createRepository(projects, destination));
			} catch (MojoExecutionException | MojoFailureException | IOException | RuntimeException e) {
				repository.completeExceptionally(e);
			}
		}
		if (!repository.isDone()) {
			throw new MojoFailureException("Can't find specified repository type provider '" + repositoryType
					+ "' please make sure it is enabled for this project!");
		}
		Artifact artifact = project.getArtifact();
		try {
			File result = repository.get();
			if (result.isDirectory()) {
				// we need to package the directory first!
				File destFile = new File(destination, project.getArtifactId() + "-" + result.getName() + ".zip");
				zipArchiver.addDirectory(result);
				zipArchiver.setDestFile(destFile);
				zipArchiver.createArchive();
				artifact.setFile(destFile);
				artifact.setArtifactHandler(new DefaultArtifactHandler("zip"));
			} else {
				artifact.setFile(result);
				artifact.setArtifactHandler(new DefaultArtifactHandler(FilenameUtils.getExtension(result.getName())));
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof MojoExecutionException mee) {
				throw mee;
			}
			if (cause instanceof MojoFailureException mfe) {
				throw mfe;
			}
			throw new MojoExecutionException("create repository failed!", e);
		} catch (IOException e) {
			throw new MojoExecutionException("package repository failed!", e);
		}

	}

}
