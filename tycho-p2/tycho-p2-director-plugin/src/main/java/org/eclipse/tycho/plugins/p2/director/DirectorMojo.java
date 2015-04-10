/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.p2.facade.RepositoryReferenceTool;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.director.shared.DirectorCommandException;
import org.eclipse.tycho.p2.tools.director.shared.DirectorRuntime;
import org.eclipse.tycho.plugins.p2.director.runtime.DirectorVersion;
import org.eclipse.tycho.plugins.p2.director.runtime.StandaloneDirectorRuntimeFactory;

/**
 * <p>
 * Creates product installations for the products defined in the project.
 * </p>
 */
// TODO 348586 should be called assemble-product
@Mojo(name = "materialize-products", defaultPhase = LifecyclePhase.PACKAGE)
public final class DirectorMojo extends AbstractProductMojo {

    public enum InstallationSource {
        targetPlatform, repository
    }

    public enum DirectorRuntimeType {
        internal, standalone
    }

    @Component
    private EquinoxServiceFactory osgiServices;

    @Component
    private RepositoryReferenceTool repositoryReferenceTool;

    @Component
    private StandaloneDirectorRuntimeFactory standaloneDirectorFactory;

    // TODO rename to profileName
    /**
     * The name of the p2 profile to be created.
     */
    @Parameter(defaultValue = "DefaultProfile")
    private String profile;

    // TODO 405785 the syntax of this parameter doesn't work well with configuration inheritance; replace with new generic envSpecificConfiguration parameter syntax
    @Parameter
    private List<ProfileName> profileNames;

    /**
     * Include the feature JARs in installation. (Technically, this sets the property
     * <tt>org.eclipse.update.install.features</tt> to <tt>true</tt> in the p2 profile.)
     */
    @Parameter(defaultValue = "true")
    private boolean installFeatures;

    /**
     * Source repositories to be used in the director calls. Can be
     * <ul>
     * <li><code>targetPlatform</code> - to use the target platform as source (default)</li>
     * <li><code>repository</code> - to use the p2 repository in <tt>target/repository/</tt> as
     * source. With this option, the build implicitly verifies that it would also be possible to
     * install the product from that repository with an external director application.
     * </ul>
     */
    @Parameter(defaultValue = "targetPlatform")
    private InstallationSource source;

    /**
     * Runtime in which the director application is executed. Can be
     * <ul>
     * <li><code>internal</code> - to use the director application from Tycho's embedded OSGi
     * runtime (default)</li>
     * <li><code>standalone</code> - to create and use a stand-alone installation of the director
     * application. This option is needed if the product to be installed includes artifacts with
     * meta-requirements (e.g. to a non-standard touchpoint action). Requires that the
     * <code>source</code> parameter is set to <code>repository</code>.
     * </ul>
     */
    @Parameter(defaultValue = "internal")
    private DirectorRuntimeType directorRuntime;

    /**
     * Version of the director application to be used. Can be
     * <ul>
     * <li><code>marsMacLayout</code> - for the current version of the director application
     * (default)</li>
     * <li><code>legacyMacLayout</code> - for the director application from Eclipse Luna (4.4),
     * which still supports installations for MacOS with the legacy file system layout. Requires
     * that the <code>directorRuntime</code> parameter is set to <code>standalone</code>.</li>
     * </ul>
     */
    @Parameter(defaultValue = "marsMacLayout")
    private DirectorVersion directorVersion;

    // TODO extract methods
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<Product> products = getProductConfig().getProducts();
        if (products.isEmpty()) {
            getLog().info("No product definitions found. Nothing to do.");
        }
        DirectorRuntime director = getDirectorRuntime();
        RepositoryReferences sources = getSourceRepositories();
        for (Product product : products) {
            for (TargetEnvironment env : getEnvironments()) {
                DirectorRuntime.Command command = director.newInstallCommand();

                File destination = getProductMaterializeDirectory(product, env);
                String rootFolder = product.getRootFolder(env.getOs());
                if (rootFolder != null && rootFolder.length() > 0) {
                    destination = new File(destination, rootFolder);
                }

                command.addMetadataSources(sources.getMetadataRepositories());
                command.addArtifactSources(sources.getArtifactRepositories());
                command.addUnitToInstall(product.getId());
                for (DependencySeed seed : product.getAdditionalInstallationSeeds()) {
                    command.addUnitToInstall(seed);
                }
                command.setDestination(destination);
                command.setProfileName(ProfileName.getNameForEnvironment(env, profileNames, profile));
                command.setEnvironment(env);
                command.setInstallFeatures(installFeatures);
                getLog().info(
                        "Installing product " + product.getId() + " for environment " + env + " to "
                                + destination.getAbsolutePath());

                try {
                    command.execute();
                } catch (DirectorCommandException e) {
                    throw new MojoFailureException("Installation of product " + product.getId() + " for environment "
                            + env + " failed", e);
                }
            }
        }
    }

    private DirectorRuntime getDirectorRuntime() throws MojoFailureException, MojoExecutionException {
        switch (directorRuntime) {
        case internal:
            if (directorVersion != DirectorVersion.marsMacLayout) {
                getLog().warn(
                        "Ignoring 'directorVersion' configuration because attribute 'directorRuntime' is not 'standalone'");
            }

            // director from Tycho's OSGi runtime
            return osgiServices.getService(DirectorRuntime.class);

        case standalone:
            // separate director installation in the target folder
            return standaloneDirectorFactory.createStandaloneDirector(directorVersion,
                    getBuildDirectory().getChild("director"), getForkedProcessTimeoutInSeconds(), getSession());

        default:
            throw new MojoFailureException("Unsupported value for attribute 'directorRuntime': \"" + directorRuntime
                    + "\"");
        }
    }

    private RepositoryReferences getSourceRepositories() throws MojoExecutionException, MojoFailureException {
        switch (source) {
        case targetPlatform:
            return getTargetPlatformRepositories();

        case repository:
            return getBuildOutputRepository();

        default:
            throw new MojoFailureException("Unsupported value for attribute 'source': \"" + source + "\"");
        }
    }

    private RepositoryReferences getBuildOutputRepository() {
        // TODO share "repository" constant?
        File buildOutputRepository = getBuildDirectory().getChild("repository");

        RepositoryReferences result = new RepositoryReferences();
        result.addMetadataRepository(buildOutputRepository);
        result.addArtifactRepository(buildOutputRepository);
        return result;
    }

    private RepositoryReferences getTargetPlatformRepositories() throws MojoExecutionException, MojoFailureException {
        int flags = RepositoryReferenceTool.REPOSITORIES_INCLUDE_CURRENT_MODULE;
        return repositoryReferenceTool.getVisibleRepositories(getProject(), getSession(), flags);
    }
}
