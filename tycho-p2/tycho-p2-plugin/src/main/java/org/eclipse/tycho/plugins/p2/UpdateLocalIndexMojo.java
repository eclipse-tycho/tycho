/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.plugins.p2;

import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.osgi.TychoServiceFactory;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;

@Mojo(name = "update-local-index", threadSafe = true)
public class UpdateLocalIndexMojo extends AbstractMojo {
    private static final Object LOCK = new Object();

    @Parameter(property = "project", readonly = true, required = true)
    private MavenProject project;

    @Component(hint = TychoServiceFactory.HINT)
    private EquinoxServiceFactory serviceFactory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        synchronized (LOCK) {
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
    }

    private void addGavAndSave(GAV gav, TychoRepositoryIndex index) throws IOException {
        index.addGav(gav);
        index.save();
    }

}
