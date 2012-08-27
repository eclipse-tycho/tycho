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
package org.eclipse.tycho.versions;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.tycho.versions.engine.ProjectMetadataReader;
import org.eclipse.tycho.versions.engine.Versions;
import org.eclipse.tycho.versions.engine.VersionsEngine;

/**
 * @author igor
 * @goal set-version
 * @aggregator
 * @requiresProject true
 * @requiresDirectInvocation true
 */
public class SetMojo extends AbstractVersionsMojo {
    /**
     * The new version number to set.
     * 
     * @parameter expression="${newVersion}"
     * @required
     */
    private String newVersion;

    /**
     * Comma separated list of artifact ids to set the new version to.
     * <p/>
     * By default, the new version will be set on the current project and all references to the
     * project, including all <parent/> elements if the project is a parent pom.
     * 
     * @parameter expression="${artifacts}" default-value="${project.artifactId}"
     */
    private String artifacts;

    /**
     * If this parameter is true, then all exported packages will be set to the given ${newVersion}
     * parameter.
     * <p/>
     * By default the export-package header will not be updated.
     * 
     * @parameter expression="{updateExportPacakge} default-value="false"
     */
    private boolean updateExportPackage;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (newVersion == null || newVersion.length() == 0) {
            throw new MojoExecutionException("Missing required parameter newVersion");
        }
        try {
            Versions.assertIsOsgiVersion(Versions.toCanonicalVersion(newVersion));
        } catch (RuntimeException e) {
            throw new MojoExecutionException("Invalid version: " + newVersion, e);
        }

        VersionsEngine engine = newEngine();
        ProjectMetadataReader metadataReader = newProjectMetadataReader();

        try {
            metadataReader.addBasedir(session.getCurrentProject().getBasedir());

            engine.setProjects(metadataReader.getProjects());

            // initial changes
            StringTokenizer st = new StringTokenizer(artifacts, ",");
            while (st.hasMoreTokens()) {
                String artifactId = st.nextToken().trim();
                engine.addVersionChange(artifactId, newVersion, updateExportPackage);
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
