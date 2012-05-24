/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.equinox.launching.internal.P2ApplicationLauncher;

public abstract class AbstractP2MetadataMojo extends AbstractMojo {
    /**
     * @parameter expression="${project}"
     * @required
     */
    protected MavenProject project;

    /**
     * Metadata repository name
     * 
     * @parameter default-value="${project.name}"
     * @required
     */
    protected String metadataRepositoryName;

    /**
     * Generated update site location (must match update-site mojo configuration)
     * 
     * @parameter expression="${project.build.directory}/site"
     */
    protected File target;

    /**
     * Artifact repository name
     * 
     * @parameter default-value="${project.name} Artifacts"
     * @required
     */
    protected String artifactRepositoryName;

    /**
     * Kill the forked test process after a certain number of seconds. If set to 0, wait forever for
     * the process, never timing out.
     * 
     * @parameter expression="${p2.timeout}"
     */
    private int forkedProcessTimeoutInSeconds;

    /**
     * Arbitrary JVM options to set on the command line.
     * 
     * @parameter
     */
    private String argLine;

    /**
     * @parameter default-value="true"
     */
    protected boolean generateP2Metadata;

    /**
     * @parameter default-value="true"
     */
    private boolean compressRepository;

    /** @component */
    private P2ApplicationLauncher launcher;

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

        if (argLine != null && argLine.trim().length() > 0) {
            // TODO does this really do anything???
            launcher.addArguments("-vmargs", argLine);
        }

        int result = launcher.execute(forkedProcessTimeoutInSeconds);
        if (result != 0) {
            throw new MojoFailureException("P2 publisher return code was " + result);
        }
    }

    protected void addArguments(P2ApplicationLauncher launcher) throws IOException, MalformedURLException {
        launcher.addArguments("-source", getUpdateSiteLocation().getCanonicalPath(), //
                "-metadataRepository", getUpdateSiteLocation().toURL().toExternalForm(), //
                "-metadataRepositoryName", metadataRepositoryName, //
                "-artifactRepository", getUpdateSiteLocation().toURL().toExternalForm(), //
                "-artifactRepositoryName", artifactRepositoryName, //
                "-noDefaultIUs");
        if (compressRepository) {
            launcher.addArguments(new String[] { "-compress" });
        }
    }

    protected abstract String getPublisherApplication();

    protected File getUpdateSiteLocation() {
        return target;
    }
}
