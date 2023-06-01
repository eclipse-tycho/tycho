/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.repository;

import java.net.URI;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.mirroring.facade.MirrorApplicationService;

/**
 * Modify the artifact metadata of the provided p2 repository by adding extra mapping rules for
 * artifacts the can be resolved to Maven repositories so the URL under Maven repository is used for
 * fetching and artifact is not duplicated inside this repo.
 */
@Mojo(name = "remap-artifacts-to-m2-repo", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class RemapArtifactToMavenRepositoriesMojo extends AbstractRepositoryMojo {

    @Component()
    MirrorApplicationService mirrorApp;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            mirrorApp.addMavenMappingRules(getAssemblyRepositoryLocation(),
                    getProject().getRemoteArtifactRepositories().stream() //
                            .filter(artifactRepo -> artifactRepo.getLayout().getId().equals("default")) //
                            .map(ArtifactRepository::getUrl) //
                            .map(URI::create) //
                            .toArray(URI[]::new));
        } catch (FacadeException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
