/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.ArtifactDependencyWalker;
import org.eclipse.tycho.core.FeatureDescription;
import org.eclipse.tycho.core.PluginDescription;
import org.eclipse.tycho.core.TargetPlatformResolver;
import org.eclipse.tycho.core.osgitools.AbstractArtifactDependencyWalker;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.targetplatform.LocalTargetPlatformResolver;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;
import org.junit.Ignore;

@Ignore
public class ArtifactDependencyWalkerTest extends AbstractTychoMojoTestCase {
    public void testProductDepdendencies() throws Exception {
        final ArrayList<PluginDescription> plugins = new ArrayList<PluginDescription>();
        final ArrayList<FeatureDescription> features = new ArrayList<FeatureDescription>();
        walkProduct("src/test/resources/dependencywalker/plugin_based.product", plugins, features);

        assertEquals(0, features.size());

        assertEquals(1, plugins.size());
        assertEquals("bundle01", plugins.get(0).getKey().getId());
        assertEquals("0.0.1", plugins.get(0).getKey().getVersion());

        plugins.clear();
        features.clear();

        walkProduct("src/test/resources/dependencywalker/feature_based.product", plugins, features);
        assertEquals(1, features.size());
        assertEquals("feature01", features.get(0).getKey().getId());
        assertEquals("1.0.0", features.get(0).getKey().getVersion());

        assertEquals(1, plugins.size());
        assertEquals("bundle01", plugins.get(0).getKey().getId());
        assertEquals("0.0.1", plugins.get(0).getKey().getVersion());
    }

    protected void walkProduct(String productFile, final ArrayList<PluginDescription> plugins,
            final ArrayList<FeatureDescription> features) throws Exception, IOException, XmlPullParserException {
        DependencyArtifacts platform = getTargetPlatform();

        final ProductConfiguration product = ProductConfiguration.read(new File(productFile));

        ArtifactDependencyWalker walker = new AbstractArtifactDependencyWalker(platform) {
            public void walk(ArtifactDependencyVisitor visitor) {
                traverseProduct(product, visitor);
            }
        };

        walker.walk(new ArtifactDependencyVisitor() {
            @Override
            public void visitPlugin(PluginDescription plugin) {
                plugins.add(plugin);
            }

            @Override
            public boolean visitFeature(FeatureDescription feature) {
                features.add(feature);
                return true;
            }
        });
    }

    protected DependencyArtifacts getTargetPlatform() throws Exception {
        LocalTargetPlatformResolver resolver = (LocalTargetPlatformResolver) lookup(TargetPlatformResolver.class,
                LocalTargetPlatformResolver.ROLE_HINT);

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setLocalRepository(new StubArtifactRepository(System.getProperty("java.io.tmpdir")));
        MavenExecutionResult result = new DefaultMavenExecutionResult();
        DefaultRepositorySystemSession repositorySession = new DefaultRepositorySystemSession();
        MavenSession session = new MavenSession(getContainer(), repositorySession, request, result);
        session.setProjects(new ArrayList<MavenProject>());
        lookup(LegacySupport.class).setSession(session);

        MavenProject project = new MavenProject();

        resolver.setLocation(new File("src/test/resources/targetplatforms/basic"));

        DependencyArtifacts platform = resolver.resolveDependencies(session, project, null,
                DefaultReactorProject.adapt(session), null);
        return platform;
    }
}
