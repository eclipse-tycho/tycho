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

/**
 * @phase package
 * @goal materialize-products
 */
public final class DirectorMojo extends AbstractProductMojo {
    /** @component */
    private EquinoxServiceFactory p2;

    /** @parameter default-value="DefaultProfile" */
    private String profile;

    /** @parameter */
    private List<ProfileName> profileNames;

    /** @parameter default-value="true" */
    private boolean installFeatures;

    /** @component */
    private RepositoryReferenceTool repositoryReferenceTool;

    // TODO extract methods
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<Product> products = getProductConfig().getProducts();
        if (products.isEmpty()) {
            getLog().info("No product definitions found. Nothing to do.");
        }
        DirectorRuntime director = p2.getService(DirectorRuntime.class);
        for (Product product : products) {
            int flags = RepositoryReferenceTool.REPOSITORIES_INCLUDE_CURRENT_MODULE;
            RepositoryReferences sources = repositoryReferenceTool.getVisibleRepositories(getProject(), getSession(),
                    flags);
            for (TargetEnvironment env : getEnvironments()) {
                DirectorRuntime.Command command = director.newInstallCommand();

                File destination = getProductMaterializeDirectory(product, env);
                String rootFolder = product.getRootFolder();
                if (rootFolder != null && rootFolder.length() > 0) {
                    destination = new File(destination, rootFolder);
                }

                command.addMetadataSources(sources.getMetadataRepositories());
                command.addArtifactSources(sources.getArtifactRepositories());
                command.addUnitToInstall(product.getId());
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

}
