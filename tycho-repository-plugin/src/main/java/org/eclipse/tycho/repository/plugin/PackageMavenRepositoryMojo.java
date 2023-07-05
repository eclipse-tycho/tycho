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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.MavenArtifactNamespace;

import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;

/**
 * Generates an OSGi repository from the current reactor projects
 *
 */
@Mojo(name = "package-maven-repository")
public class PackageMavenRepositoryMojo extends AbstractMojo {

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

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		XMLResourceGenerator resourceGenerator = new XMLResourceGenerator();
		resourceGenerator.name(repositoryName);
		for (MavenProject project : session.getProjects()) {
			if (isInteresting(project)) {
				ResourceBuilder rb = new ResourceBuilder();
				try {
					if (rb.addFile(project.getArtifact().getFile(), new URI("mvn:" + project.getGroupId() + ":"
							+ project.getArtifactId() + ":" + project.getVersion()))) {
						CapReqBuilder identity = new CapReqBuilder(MavenArtifactNamespace.MAVEN_ARTIFACT_NAMESPACE)
								.addAttribute(MavenArtifactNamespace.CAPABILITY_GROUP_ATTRIBUTE, project.getGroupId())
								.addAttribute(MavenArtifactNamespace.MAVEN_ARTIFACT_NAMESPACE, project.getArtifactId())
								.addAttribute(MavenArtifactNamespace.CAPABILITY_VERSION_ATTRIBUTE,
										project.getVersion());
						rb.addCapability(identity);
						resourceGenerator.resource(rb.build());
						getLog().info("Adding " + project.getId());
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
			File location = new File(destination, repositoryFileName);
			resourceGenerator.save(location);
			Artifact artifact = project.getArtifact();
			artifact.setArtifactHandler(new DefaultArtifactHandler("xml"));
			artifact.setFile(location);
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
