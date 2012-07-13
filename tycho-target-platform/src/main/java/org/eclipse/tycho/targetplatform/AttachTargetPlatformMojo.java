/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mickael Istria (Red Hat). - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.targetplatform;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

/**
 * Attaches a .target file to an 'eclipse-target-platform' project
 * 
 * @goal attach-target
 * @phase package
 */
public class AttachTargetPlatformMojo extends AbstractMojo {

	/**
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * @component
	 * @readonly
	 */
	private MavenProjectHelper projectHelper;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (! this.project.getPackaging().equals(TargetPlatformConstants.TARGET_PLATFORM_PACKAGING)) {
			throw new MojoExecutionException("This goal can only perform on '" + TargetPlatformConstants.TARGET_PLATFORM_PACKAGING + "' projects.");
		}
		File targetFile = new File(this.project.getBasedir(), project.getArtifactId() + TargetPlatformConstants.FILE_EXTENSION);
		if (! targetFile.exists()) {
			throw new MojoExecutionException("Associated .target file '" + targetFile.getAbsolutePath() + "' could not be found.");
		}
		this.projectHelper.attachArtifact(this.project, TargetPlatformConstants.ATTACHED_TARGET_ARTIFACT_TYPE, this.project.getArtifactId(), targetFile);
	}
}
