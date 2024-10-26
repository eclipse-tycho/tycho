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
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.eclipse.tycho.packaging.RepositoryGenerator;
import org.eclipse.tycho.packaging.RepositoryGenerator.RepositoryConfiguration;
import org.eclipse.tycho.packaging.RepositoryGenerator.RepositoryLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Generates an OSGi repository from the current reactor projects
 *
 */
@Mojo(name = "package-repository")
public class PackageRepositoryMojo extends AbstractMojo implements RepositoryConfiguration {

	private static final XmlPlexusConfiguration NO_SETTINGS = new XmlPlexusConfiguration("settings");

	static final String DEFAULT_REPOSITORY_TYPE = OSGiRepositoryGenerator.HINT;

	static final String PARAMETER_REPOSITORY_TYPE = "repositoryType";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Parameter(property = "session", readonly = true)
	protected MavenSession session;

	/**
	 * <p>
	 * The name attribute stored in the created p2 repository.
	 * </p>
	 */
	@Parameter(defaultValue = "${project.name}")
	private String repositoryName;

	@Parameter(defaultValue = "${project.build.directory}")
	private File destination;

	@Parameter(property = "project", readonly = true)
	private MavenProject project;

	/**
	 * Specify the used layout, possible values are:
	 * <ul>
	 * <li><code>maven</code> - all artifacts are referenced with the mvn protocol
	 * and the result can be deployment to a maven repository (either local or
	 * remote)</li>
	 * <li><code>local</code> - all artifacts are copied into a folder and
	 * referenced relative to this folder, the result can be</li>
	 * </ul>
	 */
	@Parameter(defaultValue = "maven")
	private RepositoryLayout repositoryLayout;

	/**
	 * Configures the used repository type
	 */
	@Parameter(defaultValue = DEFAULT_REPOSITORY_TYPE, name = PARAMETER_REPOSITORY_TYPE)
	private String repositoryType;

	@Parameter(property = "mojoExecution", readonly = true)
	MojoExecution execution;

	/**
	 * Configures the repository type specific settings.
	 */
	@Parameter
	private PlexusConfiguration settings;

	@Inject
	private Provider<ZipArchiver> zipArchiverProvider;

	@Inject
	private Map<String, RepositoryGenerator> generators;

	@Inject
	private MavenProjectHelper mavenProjectHelper;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		RepositoryGenerator generator = generators.get(repositoryType);
		if (generator == null) {
			throw new MojoFailureException(
					"No repository implementation of type " + repositoryType + " found, available ones are "
							+ generators.keySet().stream().sorted().collect(Collectors.joining(", ")));
		}
		List<MavenProject> projects = session.getProjects().stream().filter(generator::isInteresting).toList();
		try {
			File repository = generator.createRepository(projects, this);
			String executionId = execution.getExecutionId();
			if (repository.isDirectory()) {
				File destFile = new File(destination, project.getArtifactId() + "-" + repository.getName() + ".zip");
				ZipArchiver zipArchiver = zipArchiverProvider.get();
				zipArchiver.addDirectory(repository);
				zipArchiver.setDestFile(destFile);
				zipArchiver.createArchive();
				if (executionId.startsWith("default-")) {
					Artifact artifact = project.getArtifact();
					artifact.setFile(destFile);
					artifact.setArtifactHandler(new DefaultArtifactHandler("zip"));
				} else {
					mavenProjectHelper.attachArtifact(project, "zip", executionId, destFile);
				}
			} else {
				String extension = FilenameUtils.getExtension(repository.getName());
				if (executionId.startsWith("default-")) {
					Artifact artifact = project.getArtifact();
					artifact.setArtifactHandler(new DefaultArtifactHandler(extension));
					artifact.setFile(repository);
				} else {
					mavenProjectHelper.attachArtifact(project, extension, executionId, repository);
				}
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Could not write repository!", e);
		}
	}

	@Override
	public File getDestination() {
		return destination;
	}

	@Override
	public RepositoryLayout getLayout() {
		return repositoryLayout;
	}

	@Override
	public String getRepositoryName() {
		return repositoryName;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public PlexusConfiguration getConfiguration() {
		if (settings == null) {
			return NO_SETTINGS;
		}
		return settings;
	}

}
