/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Guillaume Dufour - Support for release-process like Maven
 *******************************************************************************/
package org.eclipse.tycho.versions;

import java.io.IOException;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.tycho.versions.engine.EclipseVersionUpdater;
import org.eclipse.tycho.versions.engine.ProjectMetadataReader;

/**
 * Update Eclipse/OSGi metadata (MANIFEST.MF, feature.xml, product.xml) version to match
 * corresponding pom.xml.
 */
@Mojo(name = "update-eclipse-metadata", aggregator = true, requiresDirectInvocation = true)
public class UpdateEclipseMetadataMojo extends AbstractMojo {

    @Parameter(property = "session", readonly = true)
    private MavenSession session;

    @Component
    private ProjectMetadataReader pomReader;

    @Component
    private EclipseVersionUpdater metadataUpdater;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            pomReader.addBasedir(session.getCurrentProject().getBasedir());
            metadataUpdater.setProjects(pomReader.getProjects());
            metadataUpdater.apply();
        } catch (IOException e) {
            throw new MojoExecutionException("Could not set version", e);
        }
    }

}
