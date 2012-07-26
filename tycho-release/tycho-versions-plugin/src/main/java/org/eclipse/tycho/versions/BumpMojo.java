/*******************************************************************************
 * Copyright (c) 2012 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.tycho.versions.engine.ProjectMetadataReader;
import org.eclipse.tycho.versions.engine.VersionBump;
import org.eclipse.tycho.versions.engine.VersionsEngine;

/**
 * @author mistria
 * @goal bump-version
 * @aggregator
 * @requiresProject true
 * @requiresDirectInvocation true
 */
public class BumpMojo extends AbstractVersionsMojo {
    /**
     * A specification of how to bump verions. Examples: * 0.0.1 will increment the micro (3rd
     * segement) of version * 0.2.0 will increase by 2 the minor of version * 1.0.2 will increment
     * major, and increase by 2 micro
     * 
     * @parameter expression="${versionDiff}"
     * @required
     */
    private String versionDiff;

    /**
     * Comma separated list of artifact ids to set the new version to.
     * <p/>
     * By default, the new version will be set on the current project and all references to the
     * project, including all <parent/> elements if the project is a parent pom.
     * 
     * @parameter expression="${artifacts}" default-value="${project.artifactId}"
     */
    private String artifacts;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (this.versionDiff == null || this.versionDiff.length() == 0) {
            throw new MojoExecutionException("Missing required parameter newVersion");
        }
        VersionBump versionBump;
        try {
            versionBump = new VersionBump(this.versionDiff);
        } catch (RuntimeException e) {
            throw new MojoExecutionException("Invalid versionDiff: " + this.versionDiff, e);
        }
        if (versionBump.isNoop()) {
            getLog().info("0.0.0 version diff, skipping");
            return;
        }

        VersionsEngine engine = newEngine();
        ProjectMetadataReader metadataReader = newProjectMetadataReader();

        try {
            metadataReader.addBasedir(this.session.getCurrentProject().getBasedir());

            engine.setProjects(metadataReader.getProjects());

            // initial changes
            StringTokenizer st = new StringTokenizer(this.artifacts, ",");
            while (st.hasMoreTokens()) {
                String artifactId = st.nextToken().trim();
                engine.addVersionBump(artifactId, versionBump);
            }

            engine.apply();
        } catch (IOException e) {
            throw new MojoExecutionException("Could not set version", e);
        }
    }

    private VersionsEngine newEngine() throws MojoFailureException {
        return lookup(VersionsEngine.class);
    }
}
