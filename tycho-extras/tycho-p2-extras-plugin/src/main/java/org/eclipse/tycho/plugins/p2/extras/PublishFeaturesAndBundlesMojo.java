/*******************************************************************************
 * Copyright (c) 2010, 2022 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.eclipse.sisu.equinox.launching.internal.P2ApplicationLauncher;

/**
 * This goal invokes the feature and bundle publisher on a folder.
 * 
 * @see https://wiki.eclipse.org/Equinox/p2/Publisher#Features_And_Bundles_Publisher_Application
 */
@Mojo(name = "publish-features-and-bundles")
public class PublishFeaturesAndBundlesMojo extends AbstractMojo {
    private static String CONTENT_PUBLISHER_APP_NAME = "org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher";

    /**
     * Location of the metadata repository to write. The AssembleRepositoryMojo of
     * tycho-p2-repository-plugin will only work with the predefined default
     * ${project.build.directory}/repository.
     */
    @Parameter(defaultValue = "${project.build.directory}/repository")
    private String metadataRepositoryLocation;

    /**
     * Location of the artifact repository to write. Note: The AssembleRepositoryMojo of
     * tycho-p2-repository-plugin will only work with the predefined default
     * ${project.build.directory}/repository.
     * 
     */
    @Parameter(defaultValue = "${project.build.directory}/repository")
    private String artifactRepositoryLocation;

    /**
     * Location with features and/or plugins directories on which the features and bundles publisher
     * shall be called.
     */
    @Parameter(defaultValue = "${project.build.directory}/source")
    private String sourceLocation;

    /**
     * Create compressed jars rather than plain xml
     */
    @Parameter(defaultValue = "true")
    private boolean compress;

    /**
     * Optional flag to append artifacts to an existing repository
     */
    @Parameter(defaultValue = "false")
    private boolean append;

    /**
     * Publish artifacts to repository
     */
    @Parameter(defaultValue = "true")
    private boolean publishArtifacts;

    /**
     * Optional line of additional arguments passed to the p2 application launcher.
     */
    @Parameter(defaultValue = "")
    private String additionalArgs;

    /**
     * Kill the forked process after a certain number of seconds. If set to 0, wait forever for the
     * process, never timing out.
     */
    @Parameter(property = "p2.timeout", defaultValue = "0")
    private int forkedProcessTimeoutInSeconds;

    @Parameter(property = "project")
    private MavenProject project;

    @Component
    private P2ApplicationLauncher launcher;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        publishContent();
    }

    private void publishContent() throws MojoExecutionException, MojoFailureException {
        try {
            File sourceRepositoryDir = new File(sourceLocation).getCanonicalFile();
            File artifactRepositoryDir = new File(artifactRepositoryLocation).getCanonicalFile();
            File metadataRepositoryDir = new File(metadataRepositoryLocation).getCanonicalFile();

            List<String> contentArgs = new ArrayList<>();
            contentArgs.add("-source");
            contentArgs.add(sourceRepositoryDir.toString());

            launcher.setWorkingDirectory(project.getBasedir());
            launcher.setApplicationName(CONTENT_PUBLISHER_APP_NAME);
            launcher.addArguments("-artifactRepository", artifactRepositoryDir.toURL().toString(), //
                    "-metadataRepository", metadataRepositoryDir.toURL().toString());
            launcher.addArguments(getPublishArtifactFlag());
            launcher.addArguments(getAppendFlag());
            launcher.addArguments(getCompressFlag());
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
