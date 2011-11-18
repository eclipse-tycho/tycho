/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Oberlies - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.extras;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.cli.CommandLineUtils;

/**
 * This goal invokes the feature and bundle publisher on a folder.
 * 
 * @see http://wiki.eclipse.org/Equinox/p2/Publisher#Features_And_Bundles_Publisher_Application
 * @goal publish-features-and-bundles
 */
public class PublishFeaturesAndBundlesMojo extends AbstractMojo {
    private static String CONTENT_PUBLISHER_APP_NAME = "org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher";

    /**
     * Location of the metadata repository to write. The AssembleRepositoryMojo of
     * tycho-p2-repository-plugin will only work with the predefined default
     * ${project.build.directory}/repository.
     * 
     * @parameter default-value="${project.build.directory}/repository"
     */
    private String metadataRepositoryLocation;

    /**
     * Location of the artifact repository to write. Note: The AssembleRepositoryMojo of
     * tycho-p2-repository-plugin will only work with the predefined default
     * ${project.build.directory}/repository.
     * 
     * @parameter default-value="${project.build.directory}/repository"
     */
    private String artifactRepositoryLocation;

    /**
     * Location with features and/or plugins directories on which the features and bundles publisher
     * shall be called.
     * 
     * @parameter default-value="${project.build.directory}/source"
     */
    private String sourceLocation;

    /**
     * Create compressed jars rather than plain xml
     * 
     * @parameter default-value="true"
     */
    private boolean compress;

    /**
     * Optional flag to append artifacts to an existing repository
     * 
     * @parameter default-value="false"
     */
    private boolean append;

    /**
     * Publish artifacts to repository
     * 
     * @parameter default-value="true"
     */
    private boolean publishArtifacts;

    /**
     * Optional flag to include .pack.gz files
     * 
     * @parameter default-value="false"
     */
    private boolean reusePack200Files;

    /**
     * Optional line of additional arguments passed to the p2 application launcher.
     * 
     * @parameter default-value=""
     */
    private String additionalArgs;

    /**
     * Kill the forked process after a certain number of seconds. If set to 0, wait forever for the
     * process, never timing out.
     * 
     * @parameter expression="${p2.timeout}" default-value="0"
     */
    private int forkedProcessTimeoutInSeconds;

    /** @parameter expression="${project}" */
    private MavenProject project;

    /** @component */
    private P2ApplicationLauncher launcher;

    public void execute() throws MojoExecutionException, MojoFailureException {
        publishContent();
    }

    private void publishContent() throws MojoExecutionException, MojoFailureException {
        try {
            File sourceRepositoryDir = new File(sourceLocation).getCanonicalFile();
            File artifactRepositoryDir = new File(artifactRepositoryLocation).getCanonicalFile();
            File metadataRepositoryDir = new File(metadataRepositoryLocation).getCanonicalFile();

            List<String> contentArgs = new ArrayList<String>();
            contentArgs.add("-source");
            contentArgs.add(sourceRepositoryDir.toString());

            launcher.setWorkingDirectory(project.getBasedir());
            launcher.setApplicationName(CONTENT_PUBLISHER_APP_NAME);
            launcher.addArguments("-artifactRepository", artifactRepositoryDir.toURL().toString(), //
                    "-metadataRepository", metadataRepositoryDir.toURL().toString());
            launcher.addArguments(getPublishArtifactFlag());
            launcher.addArguments(getAppendFlag());
            launcher.addArguments(getCompressFlag());
            launcher.addArguments(getReusePack200FilesFlag());
            launcher.addArguments(getAdditionalArgs());
            launcher.addArguments(contentArgs.toArray(new String[contentArgs.size()]));

            int result = launcher.execute(forkedProcessTimeoutInSeconds);
            if (result != 0) {
                throw new MojoFailureException("P2 publisher return code was " + result);
            }
        } catch (IOException ioe) {
            throw new MojoExecutionException("Unable to execute the publisher", ioe);
        }
    }

    /**
     * @return The '-compress' flag or empty if we don't want to compress.
     */
    private String[] getCompressFlag() {
        return compress ? new String[] { "-compress" } : new String[0];
    }

    /**
     * @return The '-append' flag or empty if we don't want to append.
     */
    private String[] getAppendFlag() {
        return append ? new String[] { "-append" } : new String[0];
    }

    /**
     * @return The '-publishArtifacts' flag or empty if we don't want to publish artifacts.
     */
    private String[] getPublishArtifactFlag() {
        return publishArtifacts ? new String[] { "-publishArtifacts" } : new String[0];
    }

    /**
     * @return The '-reusePack200Files' flag or empty if we don't want to include .pack.gz files.
     */
    private String[] getReusePack200FilesFlag() {
        return reusePack200Files ? new String[] { "-reusePack200Files" } : new String[0];
    }

    /**
     * @return array of parsed space separated list of additional arguments. Empty array if not
     *         defined.
     * @throws MojoExecutionException
     *             is thrown if parsing of additional arguments fails
     */
    private String[] getAdditionalArgs() throws MojoExecutionException {
        if (additionalArgs == null || "".equals(additionalArgs)) {
            return new String[0];
        } else {
            try {
                return CommandLineUtils.translateCommandline(additionalArgs);
            } catch (Exception e) {
                throw new MojoExecutionException("Unable to translate additional arguments into command line array", e);
            }
        }
    }

}
