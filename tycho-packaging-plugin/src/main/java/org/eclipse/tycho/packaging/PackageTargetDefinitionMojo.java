/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.packaging;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Registers the target definition file &lt;artifactId&gt;.target expected in the basedir of a
 * project as maven artifact.
 * 
 */
@Mojo(name = "package-target-definition", defaultPhase = LifecyclePhase.PACKAGE)
public class PackageTargetDefinitionMojo extends AbstractMojo {

    private static final String FILE_EXTENSION = ".target";

    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
        File targetFile = new File(project.getBasedir(), project.getArtifactId() + FILE_EXTENSION);
        if (!targetFile.isFile()) {
            throw new MojoExecutionException("Expected target definition file '" + targetFile.getAbsolutePath()
                    + "' could not be found.");
        }
        project.getArtifact().setFile(targetFile);
    }
}
