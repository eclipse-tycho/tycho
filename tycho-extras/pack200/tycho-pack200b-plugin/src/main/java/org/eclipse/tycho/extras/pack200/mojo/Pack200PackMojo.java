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
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.tycho.extras.pack200.Pack200Archiver;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;

/**
 * Performs pack200 pack.
 */
@Mojo(name = "pack", defaultPhase = LifecyclePhase.PACKAGE)
@Deprecated(forRemoval = true, since = "2.3.0")
public class Pack200PackMojo extends AbstractMojo {

    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    @Parameter(property = "project.build.directory", required = true)
    private File buildDirectory;

    /**
     * Project types which this plugin supports.
     */
    @Parameter
    private List<String> supportedProjectTypes = Arrays.asList("eclipse-plugin", "eclipse-test-plugin", "jar");

    @Parameter(property = "plugin.artifacts")
    private List<Artifact> pluginArtifacts;

    /**
     * Whether to fork the pack operation in a separate process.
     * 
     * @since 0.23.0
     */
    @Parameter(defaultValue = "false")
    private boolean fork;

    @Component
    private Pack200Archiver pack200;

    @Component
    protected MavenProjectHelper projectHelper;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().warn("Pack200 actions are deprecated and scheduled for removal.");
        if (Runtime.version().feature() >= 14) {
            getLog().warn("Pack200 actions are skipped when running on Java 14 and higher.");
            return;
        }
        if (!supportedProjectTypes.contains(project.getPackaging())) {
            return;
        }

        File jarFile = project.getArtifact().getFile();
        if (!jarFile.isFile()) {
            throw new MojoExecutionException("Must at least execute ``package'' phase");
        }

        try {
            File packFile = new File(buildDirectory, jarFile.getName() + ".pack.gz");
            if (pack200.pack(pluginArtifacts, jarFile, packFile, fork)) {
                projectHelper.attachArtifact(project, RepositoryLayoutHelper.PACK200_EXTENSION,
                        RepositoryLayoutHelper.PACK200_CLASSIFIER, packFile);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Could not pack200 pack jar file " + jarFile.getAbsolutePath(), e);
        }
    }
}
