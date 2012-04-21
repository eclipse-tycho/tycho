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
import java.util.jar.Pack200.Packer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.extras.pack200.Pack200Archiver;

/**
 * Performs pack200 normalization, {@link Packer} for theory behind this.
 * 
 * @goal normalize
 * @phase package
 * @requiresProject
 */
public class Pack200NormalizeMojo extends AbstractMojo {

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
     * Name of the normalized JAR.
     * 
     * @parameter alias="jarName" expression="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * Project types which this plugin supports.
     * 
     * @parameter
     */
    private List<String> supportedProjectTypes = Arrays.asList("eclipse-plugin", "eclipse-test-plugin");

    /** @component */
    private Pack200Archiver pack200;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!supportedProjectTypes.contains(project.getPackaging())) {
            return;
        }

        File jarFile = project.getArtifact().getFile();
        if (!jarFile.isFile()) {
            throw new MojoExecutionException("Must at least execute ``package'' phase");
        }

        try {
            File packFile = File.createTempFile(jarFile.getName(), ".pack", buildDirectory);
            try {
                if (pack200.normalize(jarFile, packFile)) {
                    File normalizedFile = new File(buildDirectory, finalName + ".jar");
                    if (normalizedFile.exists()) {
                        normalizedFile.delete();
                    }
                    pack200.unpack(packFile, normalizedFile);
                    project.getArtifact().setFile(normalizedFile);
                }
            } finally {
                if (!packFile.delete()) {
                    throw new MojoExecutionException("Could not delete temporary file " + packFile.getAbsolutePath());
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Could not pack200 normalize jar file " + jarFile.getAbsolutePath(), e);
        }
    }
}
