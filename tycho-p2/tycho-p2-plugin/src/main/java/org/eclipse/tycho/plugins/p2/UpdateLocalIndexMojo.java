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
package org.eclipse.tycho.plugins.p2;

import java.io.File;
import java.io.IOException;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.p2.repository.LocalTychoRepositoryIndex;

/**
 * @goal update-local-index
 */
public class UpdateLocalIndexMojo extends AbstractMojo {
    /** @parameter expression="${session}" */
    private MavenSession session;

    /** @parameter expression="${project}" */
    private MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
        File location = new File(session.getLocalRepository().getBasedir());

        try {
            LocalTychoRepositoryIndex.addProject(location, project.getGroupId(), project.getArtifactId(), project
                    .getArtifact().getVersion());
        } catch (IOException e) {
            throw new MojoExecutionException("Could not update local repository index", e);
        }
    }

}
