/*******************************************************************************
 * Copyright (c) 2010, 2021 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Igor Fedorenko - initial API and implementation
 *    Christoph Läubrich - replace deprecated refrence
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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.artifacts.IllegalArtifactReferenceException;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfigurationStub;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2maven.repository.P2ArtifactRepositoryLayout;

/**
 * Quick&dirty way to update .product file to use latest versions of IUs available from specified
 * metadata repositories.
 */
@Mojo(name = "update-product")
public class UpdateProductMojo extends AbstractUpdateMojo {

    @Parameter(defaultValue = "${project.artifactId}.product")
    private File productFile;

    @Parameter(property = "project")
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
            try {
                p2.addDependency(ArtifactType.TYPE_ECLIPSE_PLUGIN, plugin.getId(), "0.0.0");
            } catch (IllegalArtifactReferenceException e) {
                // shouldn't happen for the constant type and version
                throw new RuntimeException(e);
            }
        }

        P2ResolutionResult result = p2.resolveMetadata(resolutionContext,
                new ExecutionEnvironmentConfigurationStub(executionEnvironment));

        Map<String, String> ius = new HashMap<>();
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
    protected File getFileToBeUpdated() {
        return productFile;
    }

}
