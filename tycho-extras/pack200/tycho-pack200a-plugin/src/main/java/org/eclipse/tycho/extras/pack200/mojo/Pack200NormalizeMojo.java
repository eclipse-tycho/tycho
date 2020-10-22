/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.extras.pack200.Pack200Archiver;

/**
 * Performs pack200 normalization.
 */
@Mojo(name = "normalize", defaultPhase = LifecyclePhase.PACKAGE)
public class Pack200NormalizeMojo extends AbstractMojo {

    @Parameter(property = "project", required = true)
    private MavenProject project;

    @Parameter(property = "project.build.directory", required = true)
    private File buildDirectory;

    /**
     * Name of the normalized JAR.
     */
    @Parameter(alias = "jarName", property = "project.build.finalName", required = true)
    private String finalName;

    /**
     * Skip execution.
     * 
     * @since 0.20.0
     */
    @Parameter(defaultValue = "false")
    private boolean skip;

    /**
     * Whether to fork the pack operation in a separate process.
     * 
     * @since 0.23.0
     */
    @Parameter(defaultValue = "false")
    private boolean fork;

    /**
     * Project types which this plugin supports.
     */
    @Parameter
    private List<String> supportedProjectTypes = Arrays.asList("eclipse-plugin", "eclipse-test-plugin", "jar");

    @Parameter(property = "plugin.artifacts")
    private List<Artifact> pluginArtifacts;

    @Component
    private Pack200Archiver pack200;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (this.skip || !supportedProjectTypes.contains(project.getPackaging())) {
            return;
        }
        if (Runtime.version().feature() >= 14) {
            getLog().warn("pack200 actions are skipped when running on Java 14 and higher");
            return;
        }

        File jarFile = project.getArtifact().getFile();
        if (!jarFile.isFile()) {
            throw new MojoExecutionException("Must at least execute ``package'' phase");
        }

        try {
            File packFile = File.createTempFile(jarFile.getName(), ".pack", buildDirectory);
            try {
                if (pack200.normalize(pluginArtifacts, jarFile, packFile, fork)) {
                    File normalizedFile = new File(buildDirectory, finalName + ".jar");
                    if (normalizedFile.exists()) {
                        normalizedFile.delete();
                    }
                    pack200.unpack(pluginArtifacts, packFile, normalizedFile, fork);
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
