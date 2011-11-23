/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
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
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.tycho.core.TargetEnvironment;
import org.eclipse.tycho.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.p2.facade.RepositoryReferenceTool;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.director.facade.DirectorApplicationWrapper;

/**
 * @phase package
 * @goal materialize-products
 */
@SuppressWarnings("nls")
public final class DirectorMojo extends AbstractProductMojo {
    /** @component */
    private EquinoxServiceFactory p2;

    /** @parameter default-value="DefaultProfile" */
    private String profile;

    /* @parameter */
    private List<ProfileName> profileNames;

    /** @parameter default-value="true" */
    private boolean installFeatures;

    /** @component */
    private RepositoryReferenceTool repositoryReferenceTool;

    public void execute() throws MojoExecutionException, MojoFailureException {
        List<Product> products = getProductConfig().getProducts();
        if (products.isEmpty()) {
            getLog().info("No product definitions found. Nothing to do.");
        }
        for (Product product : products) {
            for (TargetEnvironment env : getEnvironments()) {
                final DirectorApplicationWrapper director = p2.getService(DirectorApplicationWrapper.class);
                int flags = RepositoryReferenceTool.REPOSITORIES_INCLUDE_CURRENT_MODULE;
                RepositoryReferences sources = repositoryReferenceTool.getVisibleRepositories(getProject(),
                        getSession(), flags);

                File destination = getProductMaterializeDirectory(product, env);
                String rootFolder = product.getRootFolder();
                if (rootFolder != null && rootFolder.length() > 0) {
                    destination = new File(destination, rootFolder);
                }

                String metadataRepositoryURLs = toCommaSeparatedList(sources.getMetadataRepositories());
                String artifactRepositoryURLs = toCommaSeparatedList(sources.getArtifactRepositories());
                String[] args = new String[] { "-metadatarepository", metadataRepositoryURLs, //
                        "-artifactrepository", artifactRepositoryURLs, //
                        "-installIU", product.getId(), //
                        "-destination", destination.getAbsolutePath(), //
                        "-profile", ProfileName.getNameForEnvironment(env, profileNames, profile), //
                        "-profileProperties", "org.eclipse.update.install.features=" + String.valueOf(installFeatures), //
                        "-roaming", //
                        "-p2.os", env.getOs(), "-p2.ws", env.getWs(), "-p2.arch", env.getArch() };
                getLog().info("Calling director with arguments: " + Arrays.toString(args));
                final Object result = director.run(args);
                if (!DirectorApplicationWrapper.EXIT_OK.equals(result)) {
                    throw new MojoFailureException("P2 director return code was " + result);
                }
            }
        }
    }

    private String toCommaSeparatedList(List<URI> repositories) {
        if (repositories.size() == 0) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (URI uri : repositories) {
            result.append(uri.toString());
            result.append(',');
        }
        result.setLength(result.length() - 1);
        return result.toString();
    }
}
