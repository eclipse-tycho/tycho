/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.testing;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;
import org.apache.maven.session.scope.internal.SessionScope;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.DefaultContext;
import org.codehaus.plexus.testing.PlexusExtension;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * A base class for Tycho tests that require Plexus components. This class uses the modern
 * plexus-testing library's PlexusExtension internally but maintains JUnit 4 compatibility.
 *
 */
public class TychoPlexusTestCase {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private PlexusContainer container;

    @After
    public void tearDown() throws Exception {
        if (container != null) {
            SessionScope sessionScope = container.lookup(SessionScope.class);
            sessionScope.exit();
            container.dispose();
            container = null;
        }
    }

    protected PlexusContainer getContainer() {
        if (container == null) {
            try {
                setupContainer();
            } catch (Exception e) {
                throw new RuntimeException("Failed to setup Plexus container", e);
            }
        }
        return container;
    }

    private void setupContainer() throws Exception {
        // Setup container similar to PlexusExtension
        DefaultContext plexusContext = new DefaultContext();
        plexusContext.put("basedir", getBasedir());
        
        File plexusHome = new File(getBasedir(), "target/plexus-home");
        if (!plexusHome.isDirectory()) {
            plexusHome.mkdirs();
        }
        plexusContext.put("plexus.home", plexusHome.getAbsolutePath());

        ContainerConfiguration containerConfiguration = new DefaultContainerConfiguration()
                .setName("test")
                .setContext(plexusContext.getContextData());

        customizeContainerConfiguration(containerConfiguration);

        container = new DefaultPlexusContainer(containerConfiguration);
        container.addComponent(container, PlexusContainer.class.getName());
        
        // Inject dependencies into this test instance
        ((DefaultPlexusContainer) container).addPlexusInjector(
                Collections.emptyList(), binder -> binder.requestInjection(this));
    }

    @SuppressWarnings("deprecation")
    @Before
    public void setUpServiceAndSession() throws Exception {
        if (container == null) {
            setupContainer();
        }
        
        LegacySupport legacySupport = lookup(LegacySupport.class);
        Settings settings = new Settings();
        ArtifactRepository localRepository = new StubArtifactRepository(temporaryFolder.newFolder().getAbsolutePath()) {
            DefaultRepositoryLayout layout = new DefaultRepositoryLayout();

            @Override
            public String pathOf(Artifact artifact) {
                return layout.pathOf(artifact);
            }
        };
        MavenSession mavenSession = new MavenSession(container, settings, localRepository, null, null, List.of(),
                temporaryFolder.newFolder().getAbsolutePath(), System.getProperties(), System.getProperties(),
                new Date());
        SessionScope sessionScope = container.lookup(SessionScope.class);
        mavenSession.setProjects(Collections.emptyList());
        sessionScope.enter();
        sessionScope.seed(MavenSession.class, mavenSession);
        legacySupport.setSession(mavenSession);
        modifySession(mavenSession);
        //if possible, init the service factory and loading of services
        Collection<EquinoxServiceFactory> coreFactory = lookupList(EquinoxServiceFactory.class);
        for (EquinoxServiceFactory factory : coreFactory) {
            try {
                factory.getService(Object.class);
            } catch (Exception e) {
            }
        }
    }

    protected void customizeContainerConfiguration(ContainerConfiguration containerConfiguration) {
        containerConfiguration.setAutoWiring(true);
        containerConfiguration.setClassPathScanning(PlexusConstants.SCANNING_INDEX);
    }

    public static String getBasedir() {
        return PlexusExtension.getBasedir();
    }

    protected void modifySession(MavenSession mavenSession) {

    }

    public final <T> T lookup(final Class<T> role) throws ComponentLookupException {
        return container.lookup(role);
    }

    public final <T> T lookup(final Class<T> role, String hint) throws ComponentLookupException {
        return container.lookup(role, hint);
    }

    public final <T> Collection<T> lookupList(final Class<T> role) throws ComponentLookupException {
        return container.lookupList(role);
    }

    public static File resourceFile(String path) {
        File resolvedFile = new File("src/test/resources", path).getAbsoluteFile();

        if (!resolvedFile.canRead()) {
            throw new IllegalStateException(
                    "Test resource \"" + path + "\" not found under \"src/test/resources\" in the project");
        }
        return resolvedFile;
    }

    protected static File getBasedir(String name) throws IOException {
        return TestUtil.getBasedir(name);
    }

}
