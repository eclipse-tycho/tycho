/*******************************************************************************
 * Copyright (c) 2011, 2017 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
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
import org.eclipse.tycho.versions.engine.PomVersionUpdater;
import org.eclipse.tycho.versions.engine.ProjectMetadataReader;

/**
 * Update pom.xml version to match corresponding Eclipse/OSGi metadata.
 * 
 * @author igor
 */
@Mojo(name = "update-pom", aggregator = true, requiresDirectInvocation = true, threadSafe = true)
public class UpdatePomMojo extends AbstractMojo {
    private static final Object LOCK = new Object();

    @Parameter(property = "session", readonly = true)
    protected MavenSession session;

    @Component
    protected ProjectMetadataReader pomReader;

    @Component
    private PomVersionUpdater pomUpdater;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        synchronized (LOCK) {
            try {
                pomReader.addBasedir(session.getCurrentProject().getBasedir());
                pomUpdater.setProjects(pomReader.getProjects());
                pomUpdater.apply();
            } catch (IOException e) {
                throw new MojoExecutionException("Could not set version", e);
            }
        }
    }

}
