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

import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;

/**
 * @goal update-local-index
 */
public class UpdateLocalIndexMojo extends AbstractMojo {

    /** @parameter expression="${project}" */
    private MavenProject project;

    /** @component */
    private EquinoxServiceFactory serviceFactory;

    public void execute() throws MojoExecutionException, MojoFailureException {
        LocalRepositoryP2Indices localRepoIndices = serviceFactory.getService(LocalRepositoryP2Indices.class);
        GAV gav = new GAV(project.getGroupId(), project.getArtifactId(), project.getArtifact().getVersion());
        TychoRepositoryIndex artifactsIndex = localRepoIndices.getArtifactsIndex();
        TychoRepositoryIndex metadataIndex = localRepoIndices.getMetadataIndex();
        try {
            addGavAndSave(gav, artifactsIndex);
            addGavAndSave(gav, metadataIndex);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not update local repository index", e);
        }
    }

    private void addGavAndSave(GAV gav, TychoRepositoryIndex index) throws IOException {
        index.addGav(gav);
        index.save();
    }

}
