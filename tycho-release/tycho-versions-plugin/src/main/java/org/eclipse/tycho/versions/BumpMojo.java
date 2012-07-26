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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.tycho.versions.engine.ProjectMetadata;
import org.eclipse.tycho.versions.engine.ProjectMetadataReader;
import org.eclipse.tycho.versions.engine.VersionBump;
import org.eclipse.tycho.versions.engine.VersionChange;
import org.eclipse.tycho.versions.engine.VersionsEngine;
import org.eclipse.tycho.versions.pom.MutablePomFile;

/**
 * @author mistria
 * @goal bump-version
 * @aggregator
 * @requiresProject true
 * @requiresDirectInvocation true
 */
public class BumpMojo extends AbstractVersionsMojo {
    /**
     * A specification of how to bump verions. Examples:
     * <ul>
     * <li>0.0.1 will increment the micro (3rd segement) of version</li>
     * <li>0.2.0 will increase by 2 the minor of version</li>
     * <li>1.0.2 will increment major, and increase by 2 micro</li>
     * </ul>
     * 
     * @parameter expression="${versionDiff}"
     * @required
     */
    private String versionDiff;

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
            for (ProjectMetadata project : metadataReader.getProjects()) {
                MutablePomFile pom = project.getMetadata(MutablePomFile.class);
                String newVersion = versionBump.applyTo(pom.getEffectiveVersion());
                getLog().info(pom.getArtifactId() + " => " + newVersion);
                engine.addVersionChange(new VersionChange(pom, newVersion));
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
