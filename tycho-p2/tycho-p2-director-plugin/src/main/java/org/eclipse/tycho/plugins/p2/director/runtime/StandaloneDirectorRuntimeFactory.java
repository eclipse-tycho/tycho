/*******************************************************************************
 * Copyright (c) 2012 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director.runtime;

import java.io.File;
import java.net.URI;
import java.util.Arrays;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.sisu.equinox.launching.EquinoxLauncher;
import org.eclipse.tycho.core.maven.MavenArtifactResolver;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.core.utils.TychoVersion;
import org.eclipse.tycho.p2.tools.director.shared.DirectorCommandException;
import org.eclipse.tycho.p2.tools.director.shared.DirectorRuntime;

@Component(role = StandaloneDirectorRuntimeFactory.class)
public class StandaloneDirectorRuntimeFactory {

    @Requirement
    private MavenArtifactResolver mavenArtifactResolver;

    @Requirement
    private EquinoxServiceFactory osgiServices;

    @Requirement
    private EquinoxLauncher launchHelper;

    @Requirement
    private Logger logger;

    public StandaloneDirectorRuntime createStandaloneDirector(DirectorVersion directorVersion, File installLocation,
            int forkedProcessTimeoutInSeconds, MavenSession session) throws MojoFailureException,
            MojoExecutionException {

        installStandaloneDirector(directorVersion, installLocation, session);
        return new StandaloneDirectorRuntime(installLocation, launchHelper, forkedProcessTimeoutInSeconds, logger);
    }

    private void installStandaloneDirector(DirectorVersion directorVersion, File installLocation, MavenSession session)
            throws MojoFailureException, MojoExecutionException {
        // using the internal director...
        DirectorRuntime bootstrapDirector = osgiServices.getService(DirectorRuntime.class);

        try {
            // ... install from a zipped p2 repository obtained via Maven ...
            URI directorRuntimeRepo = URI.create("jar:" + getDirectorRepositoryZip(directorVersion, session).toURI()
                    + "!/");
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

    private File getDirectorRepositoryZip(DirectorVersion directorVersion, MavenSession session)
            throws MojoFailureException, MojoExecutionException {
        String tychoVersionOfDirectorRepositoryZip = directorVersion.availableInTychoVersion;
        if (tychoVersionOfDirectorRepositoryZip == null) {
            // current Tycho version
            tychoVersionOfDirectorRepositoryZip = TychoVersion.getTychoVersion();
        }

        Dependency repositoryZipReference = new Dependency();
        repositoryZipReference.setGroupId("org.eclipse.tycho");
        repositoryZipReference.setArtifactId("tycho-standalone-p2-director");
        repositoryZipReference.setVersion(tychoVersionOfDirectorRepositoryZip);
        repositoryZipReference.setType("zip");

        return mavenArtifactResolver.getPluginArtifact(repositoryZipReference, session);
    }

}
