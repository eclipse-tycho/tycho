/*******************************************************************************
 * Copyright (c) 2010, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versionbump;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.core.p2.P2ArtifactRepositoryLayout;
import org.eclipse.tycho.core.resolver.shared.MavenRepositoryLocation;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;

/**
 * Quick&dirty way to update .product file to use latest versions of IUs available from specified
 * metadata repositories.
 * 
 * @goal update-product
 */
public class UpdateProductMojo extends AbstractUpdateMojo {
    /**
     * @parameter expression="${project.artifactId}.product"
     */
    private File productFile;

    /**
     * @parameter expression="${project}"
     */
    private MavenProject project;

    @Override
    protected void doUpdate() throws IOException, URISyntaxException {

        for (ArtifactRepository repository : project.getRemoteArtifactRepositories()) {
            URI uri = new URL(repository.getUrl()).toURI();
            if (repository.getLayout() instanceof P2ArtifactRepositoryLayout) {
                resolutionContext.addP2Repository(new MavenRepositoryLocation(repository.getId(), uri));
            }
        }

        ProductConfiguration product = ProductConfiguration.read(productFile);

        for (PluginRef plugin : product.getPlugins()) {
            p2.addDependency(ArtifactKey.TYPE_ECLIPSE_PLUGIN, plugin.getId(), "0.0.0");
        }

        P2ResolutionResult result = p2.resolveMetadata(resolutionContext);

        Map<String, String> ius = new HashMap<String, String>();
        for (P2ResolutionResult.Entry entry : result.getArtifacts()) {
            ius.put(entry.getId(), entry.getVersion());
        }

        for (PluginRef plugin : product.getPlugins()) {
            String version = ius.get(plugin.getId());
            if (version != null) {
                plugin.setVersion(version);
            }
        }

        ProductConfiguration.write(product, productFile);
    }

    @Override
    protected File getTargetFile() {
        return productFile;
    }

}
