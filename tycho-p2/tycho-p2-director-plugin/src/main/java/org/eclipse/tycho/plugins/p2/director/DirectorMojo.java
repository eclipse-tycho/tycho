/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.p2.facade.RepositoryReferenceTool;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.director.shared.DirectorCommandException;
import org.eclipse.tycho.p2.tools.director.shared.DirectorRuntime;
import org.eclipse.tycho.plugins.p2.director.runtime.StandaloneDirectorRuntimeFactory;

/**
 * @phase package
 * @goal materialize-products
 */
public final class DirectorMojo extends AbstractProductMojo {

    public enum InstallationSource {
        targetPlatform, repository
    }

    public enum DirectorRuntimeType {
        internal, standalone
    }

    /** @component */
    private EquinoxServiceFactory osgiServices;

    /** @component */
    private RepositoryReferenceTool repositoryReferenceTool;

    /** @component */
    private StandaloneDirectorRuntimeFactory standaloneDirectorFactory;

    /** @parameter default-value="DefaultProfile" */
    private String profile;

    /** @parameter */
    private List<ProfileName> profileNames;

    /** @parameter default-value="true" */
    private boolean installFeatures;

    /**
     * Installation source to be used for the director calls. Can be
     * <ul>
     * <li><code>targetPlatform</code> - to use the target platform as source (default)</li>
     * <li><code>repository</code> - to use the p2 repository in <tt>target/repository/</tt> as
     * source. This ensures that it is possible to install the product from that repository using an
     * (external) director application.
     * </ul>
     * 
     * @parameter default-value="targetPlatform"
     */
    private InstallationSource source;

    /**
     * Runtime context in which the director application is executed. Can be
     * <ul>
     * <li><code>internal</code> - to use the director application from Tycho's embedded OSGi
     * runtime (default)</li>
     * <li><code>standalone</code> - to create and use a stand-alone installation of the director
     * application. This option is needed if the product to be installed includes artifacts with
     * meta-requirements (e.g. to a non-standard touchpoint action). Requires that the
     * <code>source</code> parameter is set to <code>repository</code>.
     * </ul>
     * 
     * @parameter default-value="internal"
     */
    private DirectorRuntimeType directorRuntime;

    // TODO extract methods
    public void execute() throws MojoExecutionException, MojoFailureException {
        // TODO use values of global parameters here - currently need null here to allow for compatibility with deprecated parameter 'profileNames'
        EnvironmentSpecificConfiguration globalConfiguration = new EnvironmentSpecificConfiguration(/* profile */null,
                null);
        readEnvironmentSpecificConfiguration(globalConfiguration);

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
                command.setDestination(destination);
                command.setProfileName(getProfileNameForEnvironment(env));
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
            // director from Tycho's OSGi runtime
            return osgiServices.getService(DirectorRuntime.class);

        case standalone:
            // separate director installation in the target folder
            return standaloneDirectorFactory.createStandaloneDirector(getBuildDirectory().getChild("director"),
                    getSession().getLocalRepository());

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

    private String getProfileNameForEnvironment(TargetEnvironment env) {
        String newSpecificValue = parsedEnvSpecificConfig.getEffectiveValue(
                EnvironmentSpecificConfiguration.Parameter.PROFILE_NAME, env);
        if (newSpecificValue != null) {
            return newSpecificValue;
        }

        String oldSpecificValue = ProfileName.getNameForEnvironment(env, profileNames, /* profile */null);
        if (oldSpecificValue != null) {
            getLog().warn(
                    "The configuration parameter 'profileNames' is deprecated. Use envSpecificConfiguration/<os>[.<ws>[.<arch>]]/profileName instead");
            // TODO link to bug that traces the removal
            return oldSpecificValue;
        }

        return profile;
    }
}
