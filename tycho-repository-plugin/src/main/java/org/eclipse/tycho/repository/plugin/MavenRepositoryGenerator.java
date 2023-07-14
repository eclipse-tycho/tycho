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
import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.MavenArtifactNamespace;
import org.eclipse.tycho.helper.PluginConfigurationHelper;
import org.eclipse.tycho.helper.PluginConfigurationHelper.Configuration;
import org.eclipse.tycho.helper.ProjectHelper;
import org.eclipse.tycho.packaging.RepositoryGenerator;
import org.eclipse.tycho.repository.plugin.ConfigureMavenRepositoryMojo.ArtifactReferences;

import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;

@Component(role = RepositoryGenerator.class, hint = MavenRepositoryGenerator.OSGI_REPOSITORY)
public class MavenRepositoryGenerator implements RepositoryGenerator {

	static final String OSGI_REPOSITORY = "osgi-repository";

	@Requirement
	ProjectHelper projectHelper;

	@Requirement
	PluginConfigurationHelper configurationHelper;

	@Requirement
	Logger logger;

	@Override
	public File createRepository(List<MavenProject> projects, File destination) throws IOException {
		Configuration configuration = configurationHelper.getConfiguration(ConfigureMavenRepositoryMojo.class);

		ArtifactReferences ref = configuration
				.getEnum(ConfigureMavenRepositoryMojo.PARAMETER_REPOSITORY_LAYOUT, ArtifactReferences.class)
				.orElse(ArtifactReferences.maven);

		XMLResourceGenerator resourceGenerator = new XMLResourceGenerator();
		String repositoryName = configuration.getString(ConfigureMavenRepositoryMojo.PARAMETER_REPOSITORY_NAME)
				.or(() -> Optional.ofNullable(projectHelper.getCurrentProject()).map(MavenProject::getName))
				.orElse("repository");

		String repositoryFileName = configuration.getString(ConfigureMavenRepositoryMojo.PARAMETER_REPOSITORY_FILE_NAME)
				.orElse(ConfigureMavenRepositoryMojo.DEFAULT_REPOSITORY_FILE_NAME);
		File folder;
		if (ref == ArtifactReferences.local) {
			folder = new File(destination, FilenameUtils.getBaseName(repositoryFileName));
			folder.mkdirs();
			resourceGenerator.base(folder.toURI());
		} else {
			folder = null;
		}
		resourceGenerator.name(repositoryName);
		for (MavenProject project : projects) {
			ResourceBuilder rb = new ResourceBuilder();
			try {
				URI uri;
				File file = project.getArtifact().getFile();
				if (folder == null) {
					uri = new URI(
							"mvn:" + project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion());
				} else {
					uri = new File(folder, file.getName()).toURI();
				}
				if (rb.addFile(file, uri)) {
					CapReqBuilder identity = new CapReqBuilder(MavenArtifactNamespace.MAVEN_ARTIFACT_NAMESPACE)
							.addAttribute(MavenArtifactNamespace.CAPABILITY_GROUP_ATTRIBUTE, project.getGroupId())
							.addAttribute(MavenArtifactNamespace.MAVEN_ARTIFACT_NAMESPACE, project.getArtifactId())
							.addAttribute(MavenArtifactNamespace.CAPABILITY_VERSION_ATTRIBUTE, project.getVersion());
					rb.addCapability(identity);
					resourceGenerator.resource(rb.build());
					logger.info("Adding " + project.getId());
					if (folder != null) {
						FileUtils.copyFileToDirectory(file, folder);
					}
				} else {
					logger.info("Skip " + project.getId() + ": Not a bundle");
				}
			} catch (Exception e) {
				logger.warn("Ignoring " + project.getId() + ": " + e, logger.isDebugEnabled() ? e : null);
			}
		}
		if (folder != null) {
			File location = new File(folder, repositoryFileName);
			resourceGenerator.save(location);
			return folder;
		} else {
			File location = new File(destination, repositoryFileName);
			resourceGenerator.save(location);
			return location;
		}
	}

}
