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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.IllegalArtifactReferenceException;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfigurationStub;
import org.eclipse.tycho.core.resolver.P2ResolutionResult;
import org.eclipse.tycho.core.resolver.P2Resolver;
import org.eclipse.tycho.core.resolver.P2ResolverFactory;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.p2maven.repository.P2ArtifactRepositoryLayout;

/**
 * Quick&dirty way to update .product file to use latest versions of IUs available from specified
 * metadata repositories.
 */
@Mojo(name = "update-product")
public class UpdateProductMojo extends AbstractUpdateMojo {

    @Parameter(defaultValue = "${project.artifactId}.product")
    private File productFile;

    @Parameter(defaultValue = "JavaSE-17")
    private String executionEnvironment;

    @Component
    private P2ResolverFactory factory;

    String getExecutionEnvironment() {
        return executionEnvironment;
    }

    P2Resolver createResolver() {
        return factory.createResolver(Collections.singletonList(TargetEnvironment.getRunningEnvironment()));

    }

    @Override
    protected void doUpdate(File file) throws IOException, URISyntaxException {

        TargetPlatformConfigurationStub resolutionContext = new TargetPlatformConfigurationStub();
        for (ArtifactRepository repository : getProject().getRemoteArtifactRepositories()) {
            URI uri = new URL(repository.getUrl()).toURI();
            if (repository.getLayout() instanceof P2ArtifactRepositoryLayout) {
                resolutionContext.addP2Repository(new MavenRepositoryLocation(repository.getId(), uri));
            }
        }

        ProductConfiguration product = ProductConfiguration.read(file);

        P2Resolver resolver = createResolver();
        for (PluginRef plugin : product.getPlugins()) {
            try {
                resolver.addDependency(ArtifactType.TYPE_ECLIPSE_PLUGIN, plugin.getId(), "0.0.0");
            } catch (IllegalArtifactReferenceException e) {
                // shouldn't happen for the constant type and version
                throw new RuntimeException(e);
            }
        }

        P2ResolutionResult result = resolver.resolveMetadata(resolutionContext,
                new ExecutionEnvironmentConfigurationStub(getExecutionEnvironment()));

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

        ProductConfiguration.write(product, file);
    }

    @Override
    protected File getFileToBeUpdated() {
        return productFile;
    }

}
