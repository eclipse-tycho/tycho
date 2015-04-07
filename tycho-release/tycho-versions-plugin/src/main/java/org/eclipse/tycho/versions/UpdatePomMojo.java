/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.tycho.versions.engine.PomVersionUpdater;
import org.eclipse.tycho.versions.engine.ProjectMetadataReader;

/**
 * Update pom.xml version to match corresponding Eclipse/OSGi metadata.
 * 
 * @author igor
 */
@Mojo(name = "update-pom", aggregator = true, requiresDirectInvocation = true)
public class UpdatePomMojo extends AbstractVersionsMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        PomVersionUpdater pomUpdater = newPomUpdater();
        ProjectMetadataReader metadataReader = newProjectMetadataReader();

        try {
            metadataReader.addBasedir(session.getCurrentProject().getBasedir());

            pomUpdater.setProjects(metadataReader.getProjects());
            pomUpdater.apply();
        } catch (IOException e) {
            throw new MojoExecutionException("Could not set version", e);
        }
    }

    private PomVersionUpdater newPomUpdater() throws MojoFailureException {
        return lookup(PomVersionUpdater.class);
    }
}
