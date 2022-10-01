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

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.After;

/**
 *
 * A wrapper around {@link PlexusTestCase} that allows usage in JUnit 4/5 as well as setting the
 * classpath scanning for usage in "maven like" tests that only test plexus components.
 *
 */
public class TychoPlexusTestCase {

    PlexusTestCaseExension ext = new PlexusTestCaseExension();

    @After
    public void tearDown() {
        ext.teardownContainer();
    }

    public final <T> T lookup(final Class<T> role) throws ComponentLookupException {
        return ext.getContainer().lookup(role);
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
