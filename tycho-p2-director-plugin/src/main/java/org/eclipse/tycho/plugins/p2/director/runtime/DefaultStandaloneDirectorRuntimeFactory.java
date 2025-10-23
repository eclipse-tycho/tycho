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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.sisu.equinox.launching.EquinoxLauncher;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.p2.tools.director.shared.DirectorCommandException;
import org.eclipse.tycho.p2.tools.director.shared.DirectorRuntime;
import org.eclipse.tycho.p2.tools.publisher.PublisherActionRunner;
import org.eclipse.tycho.p2maven.repository.P2RepositoryManager;

@Named
@Singleton
public class DefaultStandaloneDirectorRuntimeFactory implements StandaloneDirectorRuntimeFactory {

    @Inject
    private RepositorySystem repositorySystem;

    @Inject
    DirectorRuntime bootstrapDirector;

    @Inject
    private EquinoxLauncher launchHelper;

    @Inject
    private Logger logger;

    @Inject
    private P2RepositoryManager repositoryManager;

    @Inject
    private IProvisioningAgent agent;

    @Override
    public StandaloneDirectorRuntime createStandaloneDirector(File installLocation, int forkedProcessTimeoutInSeconds)
            throws MojoExecutionException {

        installStandaloneDirector(installLocation);
        return new StandaloneDirectorRuntime(installLocation, launchHelper, forkedProcessTimeoutInSeconds, logger);
    }

    private void installStandaloneDirector(File installLocation) throws MojoExecutionException {
        Path productFile;
        try {
            //TODO it would be good to enhance P2 so we can read a product from a stream...
            productFile = installLocation.toPath().resolve("director.product");
            Files.createDirectories(productFile.getParent());
            try (InputStream stream = StandaloneDirectorRuntimeFactory.class.getResourceAsStream("/director.product")) {
                Files.copy(stream, productFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to extract director product!", e);
        }
        URI eclipseLatest = URI.create(TychoConstants.ECLIPSE_LATEST);
        IMetadataRepository eclipseLatestRepository;
        try {
            eclipseLatestRepository = repositoryManager.getMetadataRepository(eclipseLatest, "standalone-director");
        } catch (ProvisionException e) {
            throw new MojoExecutionException("Could not load director source repository", e);
        }
        IMetadataRepository productRepository;
        try {
            productRepository = repositoryManager
                    .createLocalMetadataRepository(installLocation.toPath().resolve("repo"), "director", Map.of());
        } catch (ProvisionException e) {
            throw new MojoExecutionException("Could not create local product repository for director application", e);
        }
        StandaloneProduct product;
        try {
            product = new StandaloneProduct(productFile.toFile(), eclipseLatestRepository);
        } catch (CoreException e) {
            throw new MojoExecutionException("Can not parse standalone director product", e);
        }
        TargetEnvironment environment = TargetEnvironment.getRunningEnvironment();
        PublisherActionRunner runner = new PublisherActionRunner(eclipseLatestRepository, List.of(environment), null);
        ProductAction productAction = new ProductAction(null, product, "tooling", null, null);
        runner.executeAction(productAction, productRepository, null);
        try {
            DirectorRuntime.Command command = bootstrapDirector.newInstallCommand("standalone");
            command.addMetadataSources(Arrays.asList(eclipseLatest, productRepository.getLocation()));
            command.addArtifactSources(Arrays.asList(eclipseLatest));

            command.addUnitToInstall("org.eclipse.equinox.launcher");
            // ... a product that includes the p2 director application ...
            command.addUnitToInstall("director");
            command.setProfileName("director");
            // ... to a location in the target folder
            command.setDestination(installLocation);
            command.setEnvironment(environment);
            logger.info("Installing a standalone p2 Director");
            command.execute();
        } catch (

        DirectorCommandException e) {
            throw new MojoExecutionException("Could not install the standalone director", e);
        }
    }

}
