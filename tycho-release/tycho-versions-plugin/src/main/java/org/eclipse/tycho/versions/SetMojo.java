/*******************************************************************************
 * Copyright (c) 2008, 2014 Sonatype Inc. and others.
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
 * <p>
 * Sets the version of the current project and child projects with the same version, and updates
 * references as necessary.
 * </p>
 * <p>
 * The set-version goal implements a version refactoring for a Tycho reactor: When updating the
 * version of a project, it consistently updates the version strings in the project's configuration
 * files (e.g. pom.xml and META-INF/MANIFEST.MF) and all references to that project (e.g. in a
 * feature.xml).
 * </p>
 * <p>
 * In many cases, the set-version goal changes the version of multiple projects or entities at once.
 * In addition to the current project, child projects with the same version are also changed. The
 * set of version changes is determined according to the following rules:
 * </p>
 * <ul>
 * <li>When the parent project of a project is changed and the project has the same version as the
 * parent project, the project is also changed.</li>
 * <li>When an <tt>eclipse-plugin</tt> project is changed and the plugin exports a package with a
 * version which is the same as the unqualified project version, the version of the package is also
 * changed.
 * <li>When an <tt>eclipse-repository</tt> project is changed and a product file in the project has
 * an equivalent version, the version in the product file is also changed.</li>
 * </ul>
 * 
 * @goal set-version
 * @aggregator
 * @requiresProject true
 * @requiresDirectInvocation true
 */
public class SetMojo extends AbstractVersionsMojo {
    /**
     * <p>
     * The new version to set to the current project and other entities which have the same version
     * as the current project.
     * </p>
     * 
     * @parameter expression="${newVersion}"
     * @required
     */
    private String newVersion;

    /**
     * <p>
     * Initial list of of projects to be changed. From these projects, the full list of projects to
     * be changed is derived according to the rules described above. If set, this parameter needs to
     * be specified as a comma separated list of artifactIds.
     * </p>
     * 
     * @parameter expression="${artifacts}" default-value="${project.artifactId}"
     */
    private String artifacts;

    /**
     * <p>
     * Comma separated list of names of POM properties to set the new version to. Note that
     * properties are only changed in the projects explicitly listed by the {@link #artifacts}
     * parameter.
     * </p>
     * 
     * @since 0.18.0
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
