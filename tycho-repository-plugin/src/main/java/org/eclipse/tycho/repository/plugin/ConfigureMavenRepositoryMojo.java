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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * This mojo can be used to configure the generation of the maven repository, it
 * can not be executed!
 */
@Mojo(name = ConfigureMavenRepositoryMojo.NAME)
public class ConfigureMavenRepositoryMojo extends AbstractMojo {

	static final String NAME = "configure-maven-repository";

	static final String PARAMETER_REPOSITORY_FILE_NAME = "repositoryFileName";

	static final String PARAMETER_REPOSITORY_NAME = "repositoryName";

	static final String PARAMETER_REPOSITORY_LAYOUT = "repositoryLayout";

	static final String DEFAULT_REPOSITORY_FILE_NAME = "repository.xml";

	private static final String DEFAULT_REPOSITORY_LAYOUT = "maven";

	public static enum ArtifactReferences {
		/**
		 * reference artifacts using mvn: protocol
		 */
		maven,
		/**
		 * references artifacts as local files and copy them into a folder
		 */
		local;
	}

	/**
	 * Specify the filename of the additionally generated OSGi Repository (if
	 * enabled)
	 */
	@Parameter(defaultValue = DEFAULT_REPOSITORY_FILE_NAME, name = PARAMETER_REPOSITORY_FILE_NAME)
	private String repositoryFileName;

	/**
	 * The name attribute stored in the created osgi repository.
	 */
	@Parameter(defaultValue = "${project.name}", name = PARAMETER_REPOSITORY_NAME)
	private String repositoryName;

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
	@Parameter(defaultValue = DEFAULT_REPOSITORY_LAYOUT, name = PARAMETER_REPOSITORY_LAYOUT)
	private ArtifactReferences repositoryLayout;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		throw new MojoFailureException("This mojo is for configuration only!");
	}

	String getRepositoryFileName() {
		return repositoryFileName;
	}

	ArtifactReferences getRepositoryLayout() {
		return repositoryLayout;
	}

	String getRepositoryName() {
		return repositoryName;
	}

}
