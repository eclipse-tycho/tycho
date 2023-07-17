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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.MavenArtifactNamespace;

import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;

/**
 * Generates an OSGi repository from the current reactor projects
 *
 */
@Mojo(name = "package-repository")
public class PackageRepositoryMojo extends AbstractMojo {

	@Parameter(property = "session", readonly = true)
	protected MavenSession session;

	/**
	 * <p>
	 * The name attribute stored in the created p2 repository.
	 * </p>
	 */
	@Parameter(defaultValue = "${project.name}")
	private String repositoryName;

	/**
	 * Specify the filename of the additionally generated OSGi Repository (if
	 * enabled)
	 */
	@Parameter(defaultValue = "repository.xml")
	private String repositoryFileName;

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

	@Component(role = Archiver.class, hint = "zip")
	private ZipArchiver zipArchiver;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		XMLResourceGenerator resourceGenerator = new XMLResourceGenerator();
		resourceGenerator.name(repositoryName);
		File folder;
		if (repositoryLayout == RepositoryLayout.local) {
			folder = new File(destination, FilenameUtils.getBaseName(repositoryFileName));
			folder.mkdirs();
			resourceGenerator.base(folder.toURI());
		} else {
			folder = null;
		}
		for (MavenProject project : session.getProjects()) {
			if (isInteresting(project)) {
				ResourceBuilder rb = new ResourceBuilder();
				try {
					URI uri;
					File file = project.getArtifact().getFile();
					if (folder == null) {
						uri = new URI("mvn:" + project.getGroupId() + ":" + project.getArtifactId() + ":"
								+ project.getVersion());
					} else {
						uri = new File(folder, file.getName()).toURI();
					}
					if (rb.addFile(project.getArtifact().getFile(), uri)) {
						CapReqBuilder identity = new CapReqBuilder(MavenArtifactNamespace.MAVEN_ARTIFACT_NAMESPACE)
								.addAttribute(MavenArtifactNamespace.CAPABILITY_GROUP_ATTRIBUTE, project.getGroupId())
								.addAttribute(MavenArtifactNamespace.MAVEN_ARTIFACT_NAMESPACE, project.getArtifactId())
								.addAttribute(MavenArtifactNamespace.CAPABILITY_VERSION_ATTRIBUTE,
										project.getVersion());
						rb.addCapability(identity);
						resourceGenerator.resource(rb.build());
						getLog().info("Adding " + project.getId());
						if (folder != null) {
							FileUtils.copyFileToDirectory(file, folder);
						}
					} else {
						getLog().info("Skip " + project.getId() + ": Not a bundle");
					}
				} catch (Exception e) {
					Log log = getLog();
					log.warn("Ignoring " + project.getId() + ": " + e, log.isDebugEnabled() ? e : null);
				}
			}
		}
		try {
			Artifact artifact = project.getArtifact();
			if (folder != null) {
				File location = new File(folder, repositoryFileName);
				resourceGenerator.save(location);
				File destFile = new File(destination, project.getArtifactId() + "-" + folder.getName() + ".zip");
				zipArchiver.addDirectory(folder);
				zipArchiver.setDestFile(destFile);
				zipArchiver.createArchive();
				artifact.setFile(destFile);
				artifact.setArtifactHandler(new DefaultArtifactHandler("zip"));
			} else {
				File location = new File(destination, repositoryFileName);
				resourceGenerator.save(location);
				artifact.setArtifactHandler(new DefaultArtifactHandler("xml"));
				artifact.setFile(location);
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Could not write OSGi Repository!", e);
		}
	}

	public static boolean isInteresting(MavenProject other) {
		String packaging = other.getPackaging();
		return "jar".equalsIgnoreCase(packaging) || "bundle".equalsIgnoreCase(packaging)
				|| ArtifactType.TYPE_ECLIPSE_PLUGIN.equals(packaging)
				|| ArtifactType.TYPE_BUNDLE_FRAGMENT.equals(packaging)
				|| ArtifactType.TYPE_ECLIPSE_TEST_PLUGIN.equals(packaging);
	}

}
