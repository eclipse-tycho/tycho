/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director.runtime;

import java.io.File;
import java.net.URI;
import java.util.Arrays;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.sisu.equinox.launching.EquinoxLauncher;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.core.utils.TychoVersion;
import org.eclipse.tycho.p2.tools.director.shared.DirectorCommandException;
import org.eclipse.tycho.p2.tools.director.shared.DirectorRuntime;

@Component(role = StandaloneDirectorRuntimeFactory.class)
public class StandaloneDirectorRuntimeFactory {

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private EquinoxServiceFactory osgiServices;

    @Requirement
    private EquinoxLauncher launchHelper;

    @Requirement
    private Logger logger;

    public StandaloneDirectorRuntime createStandaloneDirector(File installLocation,
            ArtifactRepository localMavenRepository) throws MojoExecutionException {

        installStandaloneDirector(installLocation, localMavenRepository);
        return new StandaloneDirectorRuntime(installLocation, launchHelper, logger);
    }

    private void installStandaloneDirector(File installLocation, ArtifactRepository localMavenRepository)
            throws MojoExecutionException {
        // using the internal director...
        DirectorRuntime bootstrapDirector = osgiServices.getService(DirectorRuntime.class);

        try {
            // ... install from a zipped p2 repository obtained via Maven ...
            URI directorRuntimeRepo = URI
                    .create("jar:" + getDirectorRepositoryZip(localMavenRepository).toURI() + "!/");
            DirectorRuntime.Command command = bootstrapDirector.newInstallCommand();
            command.addMetadataSources(Arrays.asList(directorRuntimeRepo));
            command.addArtifactSources(Arrays.asList(directorRuntimeRepo));

            // ... a product that includes the p2 director application ...
            command.addUnitToInstall("tycho-standalone-p2-director");
            command.setProfileName("director");

            // ... to a location in the target folder
            command.setDestination(installLocation);

            // there is only this environment in the p2 repository zip
            // TODO use a "no environment-specific units" setting
            command.setEnvironment(new TargetEnvironment("linux", "gtk", "x86_64"));

            logger.info("Installing a standalone p2 Director...");
            command.execute();
        } catch (DirectorCommandException e) {
            throw new MojoExecutionException("Could not install the standalone director", e);
        }
    }

    private File getDirectorRepositoryZip(ArtifactRepository localMavenRepository) {
        // this artifact is a dependency of the Mojo, so we expect it in the local Maven repo
        Artifact artifact = repositorySystem.createArtifact("org.eclipse.tycho", "tycho-standalone-p2-director",
                TychoVersion.getTychoVersion(), "eclipse-repository");
        return new File(localMavenRepository.getBasedir(), localMavenRepository.pathOf(artifact));
    }
}
