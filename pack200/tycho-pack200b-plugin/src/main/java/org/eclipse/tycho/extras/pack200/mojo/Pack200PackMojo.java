/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.pack200.mojo;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.tycho.extras.pack200.Pack200Archiver;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;

/**
 * Performs pack200 pack.
 * 
 * @goal pack
 * @phase package
 * @requiresProject
 */
public class Pack200PackMojo extends AbstractMojo {

    /**
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File buildDirectory;

    /**
     * Project types which this plugin supports.
     * 
     * @parameter
     */
    private List<String> supportedProjectTypes = Arrays.asList("eclipse-plugin", "eclipse-test-plugin");

    /** @parameter expression="${plugin.artifacts}" */
    private List<Artifact> pluginArtifacts;

    /** @component */
    private Pack200Archiver pack200;

    /** @component */
    protected MavenProjectHelper projectHelper;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!supportedProjectTypes.contains(project.getPackaging())) {
            return;
        }

        File jarFile = project.getArtifact().getFile();
        if (!jarFile.isFile()) {
            throw new MojoExecutionException("Must at least execute ``package'' phase");
        }

        try {
            File packFile = new File(buildDirectory, jarFile.getName() + ".pack.gz");
            if (pack200.pack(pluginArtifacts, jarFile, packFile)) {
                projectHelper.attachArtifact(project, RepositoryLayoutHelper.PACK200_EXTENSION,
                        RepositoryLayoutHelper.PACK200_CLASSIFIER, packFile);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Could not pack200 pack jar file " + jarFile.getAbsolutePath(), e);
        }
    }
}
