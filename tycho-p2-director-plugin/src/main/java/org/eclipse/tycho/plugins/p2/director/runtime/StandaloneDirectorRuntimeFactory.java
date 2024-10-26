/*******************************************************************************
 * Copyright (c) 2012 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director.runtime;

import java.io.File;
import java.net.URI;
import java.util.Arrays;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.repository.RepositorySystem;
import org.eclipse.sisu.equinox.launching.EquinoxLauncher;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.p2.tools.director.shared.DirectorCommandException;
import org.eclipse.tycho.p2.tools.director.shared.DirectorRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
public class StandaloneDirectorRuntimeFactory {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RepositorySystem repositorySystem;
    private final DirectorRuntime bootstrapDirector;
    private final EquinoxLauncher launchHelper;

    @Inject
    public StandaloneDirectorRuntimeFactory(RepositorySystem repositorySystem, DirectorRuntime bootstrapDirector, EquinoxLauncher launchHelper) {
        this.repositorySystem = repositorySystem;
        this.bootstrapDirector = bootstrapDirector;
        this.launchHelper = launchHelper;
    }

    public StandaloneDirectorRuntime createStandaloneDirector(File installLocation,
                                                              ArtifactRepository localMavenRepository, int forkedProcessTimeoutInSeconds) throws MojoExecutionException {

        installStandaloneDirector(installLocation, localMavenRepository);
        return new StandaloneDirectorRuntime(installLocation, launchHelper, forkedProcessTimeoutInSeconds, logger);
    }

    private void installStandaloneDirector(File installLocation, ArtifactRepository localMavenRepository)
            throws MojoExecutionException {
        try {
            // ... install from a zipped p2 repository obtained via Maven ...
            URI directorRuntimeRepo = URI
                    .create("jar:" + getDirectorRepositoryZip(localMavenRepository).toURI() + "!/");
            DirectorRuntime.Command command = bootstrapDirector.newInstallCommand("standalone");
            command.addMetadataSources(Arrays.asList(directorRuntimeRepo));
            command.addArtifactSources(Arrays.asList(directorRuntimeRepo));

            // ... a product that includes the p2 director application ...
            command.addUnitToInstall("tycho-bundles-external");
            command.setProfileName("director");

            // ... to a location in the target folder
            command.setDestination(installLocation);

            // there is only this environment in the p2 repository zip
            // TODO use a "no environment-specific units" setting
            command.setEnvironment(new TargetEnvironment("linux", "gtk", "x86_64"));

            logger.info("Installing a standalone p2 Director");
            command.execute();
        } catch (DirectorCommandException e) {
            throw new MojoExecutionException("Could not install the standalone director", e);
        }
    }

    private File getDirectorRepositoryZip(ArtifactRepository localMavenRepository) {
        // this artifact is a dependency of the Mojo, so we expect it in the local Maven repo
        Artifact artifact = repositorySystem.createArtifact("org.eclipse.tycho", "tycho-bundles-external", "2.7.5",
                "eclipse-repository");
        return new File(localMavenRepository.getBasedir(), localMavenRepository.pathOf(artifact));
    }
}
