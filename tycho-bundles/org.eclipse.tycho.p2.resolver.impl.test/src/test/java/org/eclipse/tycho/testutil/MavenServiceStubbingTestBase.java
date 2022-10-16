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
package org.eclipse.tycho.testutil;

import org.eclipse.core.internal.net.ProxyManager;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.tycho.MavenRepositorySettings;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.locking.facade.FileLockService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 * Test base class that provides a stub registration of those services which are normally provided
 * from outside the OSGi runtime.
 */
public class MavenServiceStubbingTestBase {

    @Rule
    public LogVerifier logVerifier = new LogVerifier();
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    // in the productive code, these services are provided from outside the OSGi runtime
    @Rule
    public StubServiceRegistration<MavenContext> mavenContextRegistration = new StubServiceRegistration<>(
            MavenContext.class);
    @Rule
    public StubServiceRegistration<MavenRepositorySettings> repositorySettingsRegistration = new StubServiceRegistration<>(
            MavenRepositorySettings.class, new MavenRepositorySettingsStub());
    @Rule
    public StubServiceRegistration<FileLockService> fileLockServiceRegistration = new StubServiceRegistration<>(
            FileLockService.class, new NoopFileLockService());

    @Rule
    public StubServiceRegistration<IProxyService> proxyServiceRegistration = new StubServiceRegistration<>(
            IProxyService.class, ProxyManager.getProxyManager());

    @Before
    public void initServiceInstances() throws Exception {
        mavenContextRegistration.registerService(createMavenContext());
    }

    private MavenContext createMavenContext() throws Exception {
        MavenContext mavenContext = new MockMavenContext(temporaryFolder.newFolder("target"), logVerifier.getLogger()) {

            @Override
            public String getExtension(String artifactType) {
                return artifactType;
            }

        };
        return mavenContext;
    }

}
