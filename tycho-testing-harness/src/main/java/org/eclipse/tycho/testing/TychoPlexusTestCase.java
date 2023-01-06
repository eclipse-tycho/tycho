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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;
import org.apache.maven.session.scope.internal.SessionScope;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * A wrapper around {@link PlexusTestCase} that allows usage in JUnit 4/5 as well as setting the
 * classpath scanning for usage in "maven like" tests that only test plexus components.
 *
 */
public class TychoPlexusTestCase {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    PlexusTestCaseExension ext = new PlexusTestCaseExension();

    @After
    public void tearDown() throws ComponentLookupException {
        SessionScope sessionScope = ext.getContainer().lookup(SessionScope.class);
        sessionScope.exit();
        ext.teardownContainer();
    }

    protected PlexusContainer getContainer() {
        return ext.getContainer();
    }

    @SuppressWarnings("deprecation")
    @Before
    public void setUpServiceAndSession() throws ComponentLookupException, IOException {
        LegacySupport legacySupport = lookup(LegacySupport.class);
        PlexusContainer container = ext.getContainer();
        Settings settings = new Settings();
        ArtifactRepository localRepository = new StubArtifactRepository(temporaryFolder.newFolder().getAbsolutePath());
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

    protected void modifySession(MavenSession mavenSession) {

    }

    public final <T> T lookup(final Class<T> role) throws ComponentLookupException {
        return ext.getContainer().lookup(role);
    }

    public final <T> T lookup(final Class<T> role, String hint) throws ComponentLookupException {
        return ext.getContainer().lookup(role, hint);
    }

    public final <T> Collection<T> lookupList(final Class<T> role) throws ComponentLookupException {
        return ext.getContainer().lookupList(role);
    }

    public static File resourceFile(String path) {
        File resolvedFile = new File("src/test/resources", path).getAbsoluteFile();

        if (!resolvedFile.canRead()) {
            throw new IllegalStateException(
                    "Test resource \"" + path + "\" not found under \"src/test/resources\" in the project");
        }
        return resolvedFile;
    }

    private static final class PlexusTestCaseExension extends PlexusTestCase {

        @Override
        protected void customizeContainerConfiguration(ContainerConfiguration configuration) {
            configuration.setAutoWiring(true);
            configuration.setClassPathScanning(PlexusConstants.SCANNING_INDEX);
        }

        @Override
        public synchronized void setupContainer() {
            super.setupContainer();
        }

        @Override
        public synchronized void teardownContainer() {
            super.teardownContainer();
        }

        @Override
        public PlexusContainer getContainer() {
            return super.getContainer();
        }

    }

}
