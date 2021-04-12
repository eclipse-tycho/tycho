/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.plugins.p2;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.equinox.launching.internal.P2ApplicationLauncher;

public abstract class AbstractP2MetadataMojo extends AbstractMojo {

    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    /**
     * Metadata repository name
     */
    @Parameter(defaultValue = "${project.name}", required = true)
    protected String metadataRepositoryName;

    /**
     * Generated update site location (must match update-site mojo configuration)
     */
    @Parameter(defaultValue = "${project.build.directory}/site")
    protected File target;

    /**
     * Artifact repository name
     */
    @Parameter(defaultValue = "${project.name} Artifacts", required = true)
    protected String artifactRepositoryName;

    /**
     * Kill the forked test process after a certain number of seconds. If set to 0, wait forever for
     * the process, never timing out.
     */
    @Parameter(property = "p2.timeout")
    private int forkedProcessTimeoutInSeconds;

    /**
     * Arbitrary JVM options to set on the command line.
     */
    @Parameter
    private String argLine;

    @Parameter(defaultValue = "true")
    protected boolean generateP2Metadata;

    @Parameter(defaultValue = "true")
    private boolean compressRepository;

    @Component
    private P2ApplicationLauncher launcher;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!generateP2Metadata) {
            return;
        }

        try {
            if (getUpdateSiteLocation().isDirectory()) {
                generateMetadata();
            } else {
                logUpdateSiteLocationNotFound();
            }
        } catch (MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Cannot generate P2 metadata", e);
        }
    }

    protected void logUpdateSiteLocationNotFound() {
        getLog().warn(getUpdateSiteLocation().getAbsolutePath() + " does not exist or is not a directory");
    }

    private void generateMetadata() throws Exception {
        P2ApplicationLauncher launcher = this.launcher;

        launcher.setWorkingDirectory(project.getBasedir());
        launcher.setApplicationName(getPublisherApplication());

        addArguments(launcher);

        if (argLine != null && !argLine.trim().isEmpty()) {
            // TODO does this really do anything???
            launcher.addArguments("-vmargs", argLine);
        }

        int result = launcher.execute(forkedProcessTimeoutInSeconds);
        if (result != 0) {
            throw new MojoFailureException("P2 publisher return code was " + result);
        }
    }

    protected void addArguments(P2ApplicationLauncher launcher) throws IOException, MalformedURLException {
        launcher.addArguments("-source", getUpdateSiteLocation().getAbsolutePath(), //
                "-metadataRepository", getUpdateSiteLocation().toURL().toExternalForm(), //
                "-metadataRepositoryName", metadataRepositoryName, //
                "-artifactRepository", getUpdateSiteLocation().toURL().toExternalForm(), //
                "-artifactRepositoryName", artifactRepositoryName, //
                "-noDefaultIUs");
        if (compressRepository) {
            launcher.addArguments("-compress");
        }
    }

    protected abstract String getPublisherApplication();

    protected File getUpdateSiteLocation() {
        return target;
    }
}
