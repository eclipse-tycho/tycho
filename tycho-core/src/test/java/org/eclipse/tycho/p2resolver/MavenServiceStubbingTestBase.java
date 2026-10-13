/*******************************************************************************
 * Copyright (c) 2013 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.extension.ExtendWith;
import org.eclipse.tycho.test.util.TychoPlexusExtension;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.io.File;
import java.util.Collection;

import javax.inject.Inject;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.FileLockService;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.MockMavenContext;
import org.eclipse.tycho.test.util.NoopFileLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test base class that provides a stub registration of those services which are normally provided
 * from outside the OSGi runtime.
 */
@ExtendWith(TychoPlexusExtension.class)
public class MavenServiceStubbingTestBase {

    @Inject
    protected PlexusContainer container;

    @TempDir
    Path temporaryFolder;

    @RegisterExtension
    public LogVerifier logVerifier = new LogVerifier();

    private IProvisioningAgent provisioningAgent;

    @BeforeEach
    public void initServiceInstances() throws Exception {
        //trigger loading of the embedded OSGi framework
        Collection<EquinoxServiceFactory> serviceFactories = container.lookupList(EquinoxServiceFactory.class);
        for (EquinoxServiceFactory factory : serviceFactories) {
            try {
                factory.getService(IProvisioningAgent.class);
            } catch (Exception e) {

            }
        }
        provisioningAgent = container.lookup(IProvisioningAgent.class);
        provisioningAgent.getService(Object.class);
        assertNotNull(provisioningAgent);
    }

    protected MavenContext createMavenContext() throws Exception {
        MavenContext mavenContext = new MockMavenContext(newFolder("target"), logVerifier.getLogger()) {

            @Override
            public String getExtension(String artifactType) {
                return artifactType;
            }

        };
        return mavenContext;
    }

    protected FileLockService getFileLockService() {
        return new NoopFileLockService();
    }

    protected IProvisioningAgent getProvisioningAgent() {
        return provisioningAgent;
    }


    private File newFolder(String path) throws IOException {
        return Files.createDirectories(temporaryFolder.resolve(path)).toFile();
    }
}
