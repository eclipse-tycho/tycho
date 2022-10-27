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

import static org.junit.Assert.assertNotNull;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.osgi.TychoServiceFactory;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.MockMavenContext;
import org.eclipse.tycho.test.util.NoopFileLockService;
import org.eclipse.tycho.testing.TychoPlexusTestCase;
import org.junit.Before;
import org.junit.Rule;

/**
 * Test base class that provides a stub registration of those services which are normally provided
 * from outside the OSGi runtime.
 */
public class MavenServiceStubbingTestBase extends TychoPlexusTestCase {

    @Rule
    public LogVerifier logVerifier = new LogVerifier();

    private IProvisioningAgent provisioningAgent;

    @Before
    public void initServiceInstances() throws Exception {
        //trigger loading of the embedded OSGi framework
        EquinoxServiceFactory serviceFactory = lookup(EquinoxServiceFactory.class, TychoServiceFactory.HINT);
        assertNotNull(serviceFactory);
        provisioningAgent = lookup(IProvisioningAgent.class);
        assertNotNull(provisioningAgent);
    }

    protected MavenContext createMavenContext() throws Exception {
        MavenContext mavenContext = new MockMavenContext(temporaryFolder.newFolder("target"), logVerifier.getLogger()) {

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

}
