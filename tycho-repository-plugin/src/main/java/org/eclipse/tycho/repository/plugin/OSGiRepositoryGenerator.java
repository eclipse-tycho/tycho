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

import static aQute.bnd.osgi.Constants.MIME_TYPE_BUNDLE;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.eclipse.tycho.MavenArtifactNamespace;
import org.eclipse.tycho.packaging.RepositoryGenerator;
import org.osgi.resource.Resource;

import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.libg.cryptography.SHA256;

@Component(role = RepositoryGenerator.class, hint = OSGiRepositoryGenerator.HINT)
public class OSGiRepositoryGenerator implements RepositoryGenerator {

	static final String HINT = "osgi";

	@Override
	public File createRepository(List<MavenProject> projects, RepositoryConfiguration repoConfig)
			throws IOException, MojoExecutionException, MojoFailureException {
		XMLResourceGenerator resourceGenerator = new XMLResourceGenerator();
		resourceGenerator.name(repoConfig.getRepositoryName());
		File folder;
		PlexusConfiguration generatorConfig = repoConfig.getConfiguration();
		String repositoryFileName = generatorConfig.getChild("repositoryFileName").getValue("repository.xml");
		if (repoConfig.getLayout() == RepositoryLayout.local) {
			String folderName = generatorConfig.getChild("repositoryFolderName")
					.getValue(FilenameUtils.getBaseName(repositoryFileName));
			folder = new File(repoConfig.getDestination(), folderName);
			folder.mkdirs();
			resourceGenerator.base(folder.toURI());
		} else {
			folder = null;
		}
		Log log = repoConfig.getLog();
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

				Resource resource = getResourceFromFile(file, uri);
				if (resource != null) {
					log.info("Adding " + project.getId());
					rb.addResource(resource);
					CapReqBuilder identity = new CapReqBuilder(MavenArtifactNamespace.MAVEN_ARTIFACT_NAMESPACE)
							.addAttribute(MavenArtifactNamespace.CAPABILITY_GROUP_ATTRIBUTE, project.getGroupId())
							.addAttribute(MavenArtifactNamespace.MAVEN_ARTIFACT_NAMESPACE, project.getArtifactId())
							.addAttribute(MavenArtifactNamespace.CAPABILITY_VERSION_ATTRIBUTE, project.getVersion());
					rb.addCapability(identity);
					resourceGenerator.resource(rb.build());
					if (folder != null) {
						FileUtils.copyFileToDirectory(file, folder);
					}
				} else {
					log.info("Skip " + project.getId() + ": Not a bundle");
				}
			} catch (Exception e) {
				log.warn("Ignoring " + project.getId() + ": " + e, log.isDebugEnabled() ? e : null);
			}
		}
		if (folder != null) {
			File location = new File(folder, repositoryFileName);
			resourceGenerator.save(location);
			return folder;
		} else {
			File location = new File(repoConfig.getDestination(), repositoryFileName);
			resourceGenerator.save(location);
			return location;
		}
	}

	private Resource getResourceFromFile(File file, URI uri) throws Exception {
		ResourceBuilder rb = new ResourceBuilder();
		Domain manifest = Domain.domain(file);
		if (manifest != null && rb.addManifest(manifest)) {
			rb.addContentCapability(uri, SHA256.digest(file).asHex(), file.length(), MIME_TYPE_BUNDLE);
			return rb.build();
		}
		return null;
	}

}
