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
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.tycho.versions.engine.ProjectMetadataReader;
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
     * Comma separated list of names of pom properties to set the new version to. Properties are
     * changed in projects identified by {@link #artifacts} parameter only.
     * 
     * @parameter expression="${properties}"
     */
    private String properties;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (newVersion == null || newVersion.length() == 0) {
            throw new MojoExecutionException("Missing required parameter newVersion");
        }

        VersionsEngine engine = newEngine();
        ProjectMetadataReader metadataReader = newProjectMetadataReader();

        try {
            metadataReader.addBasedir(session.getCurrentProject().getBasedir());

            engine.setProjects(metadataReader.getProjects());

            // initial changes
            for (String artifactId : split(artifacts)) {
                engine.addVersionChange(artifactId, newVersion);
                for (String propertyName : split(properties)) {
                    engine.addPropertyChange(artifactId, propertyName, newVersion);
                }
            }

            engine.apply();
        } catch (IOException e) {
            throw new MojoExecutionException("Could not set version", e);
        }
    }

    private VersionsEngine newEngine() throws MojoFailureException {
        return lookup(VersionsEngine.class);
    }

    private static List<String> split(String str) {
        ArrayList<String> result = new ArrayList<String>();
        if (str != null) {
            StringTokenizer st = new StringTokenizer(str, ",");
            while (st.hasMoreTokens()) {
                result.add(st.nextToken().trim());
            }
        }
        return result;
    }
}
