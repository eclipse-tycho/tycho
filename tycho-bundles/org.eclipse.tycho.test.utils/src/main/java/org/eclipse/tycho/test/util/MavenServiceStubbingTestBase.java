/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenContextImpl;
import org.eclipse.tycho.core.resolver.shared.MavenRepositorySettings;
import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.p2.remote.testutil.MavenRepositorySettingsStub;
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
    public StubServiceRegistration<MavenContext> mavenContextRegistration = new StubServiceRegistration<MavenContext>(
            MavenContext.class);
    @Rule
    public StubServiceRegistration<MavenRepositorySettings> repositorySettingsRegistration = new StubServiceRegistration<MavenRepositorySettings>(
            MavenRepositorySettings.class, new MavenRepositorySettingsStub());
    @Rule
    public StubServiceRegistration<FileLockService> fileLockServiceRegistration = new StubServiceRegistration<FileLockService>(
            FileLockService.class, new NoopFileLockService());

    @Before
    public void initServiceInstances() throws Exception {
        mavenContextRegistration.registerService(createMavenContext());
    }

    private MavenContext createMavenContext() throws Exception {
        MavenContext mavenContext = new MavenContextImpl(temporaryFolder.newFolder("target"), logVerifier.getLogger());
        return mavenContext;
    }

}
