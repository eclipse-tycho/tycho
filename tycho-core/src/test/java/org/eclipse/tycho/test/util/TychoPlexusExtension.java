/*******************************************************************************
 * Copyright (c) 2026 Aleksandar Kurtakov and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Aleksandar Kurtakov - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;
import org.apache.maven.session.scope.internal.SessionScope;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.testing.PlexusExtension;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * A {@link PlexusExtension} that additionally enters the Maven {@link SessionScope} and seeds a
 * {@link MavenSession} into it.
 * <p>
 * Plain {@code @PlexusTest} is enough for components that are not session scoped. Tycho components
 * such as {@code DefaultMavenBundleResolver} inject session scoped ones, and looking those up
 * without a scope fails with "Cannot access session scope outside of a scoping block", so use
 * {@code @ExtendWith(TychoPlexusExtension.class)} for tests that touch them.
 */
public class TychoPlexusExtension extends PlexusExtension {

    @Override
    protected void customizeContainerConfiguration(ContainerConfiguration configuration) {
        configuration.setAutoWiring(true);
        configuration.setClassPathScanning(PlexusConstants.SCANNING_INDEX);
    }

    /**
     * Implemented by tests that need to change the {@link MavenSession} before any component reads
     * it. Values such as the offline flag are cached on first access, so setting them from a
     * {@code @BeforeEach} of the test itself would come too late.
     */
    public interface MavenSessionCustomizer {
        void customizeSession(MavenSession session);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void beforeEach(ExtensionContext context) throws Exception {
        super.beforeEach(context);
        PlexusContainer container = getContainer(context);
        MavenSession mavenSession = new MavenSession(container, new Settings(),
                new StubArtifactRepository(getBasedir()), null, null, List.of(), getBasedir(),
                System.getProperties(), System.getProperties(), new Date());
        mavenSession.setProjects(List.of());
        if (context.getRequiredTestInstance() instanceof MavenSessionCustomizer customizer) {
            customizer.customizeSession(mavenSession);
        }
        SessionScope sessionScope = container.lookup(SessionScope.class);
        sessionScope.enter();
        sessionScope.seed(MavenSession.class, mavenSession);
        container.lookup(LegacySupport.class).setSession(mavenSession);
        // if possible, init the service factory and loading of services
        Collection<EquinoxServiceFactory> coreFactory = container.lookupList(EquinoxServiceFactory.class);
        for (EquinoxServiceFactory factory : coreFactory) {
            try {
                factory.getService(Object.class);
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        try {
            getContainer(context).lookup(SessionScope.class).exit();
        } finally {
            super.afterEach(context);
        }
    }

}
