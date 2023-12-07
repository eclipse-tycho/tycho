/*******************************************************************************
 * Copyright (c) 2013, 2021 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - adjust to new API 
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import static org.junit.Assert.assertNotNull;

import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.repository.registry.facade.ReactorRepositoryManager;
import org.eclipse.tycho.test.util.ReactorProjectStub;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ReactorRepositoryManagerTest extends MavenServiceStubbingTestBase {

    private ReactorRepositoryManager subject;
    @Rule
    public final TemporaryFolder tempManager = new TemporaryFolder();

    private PomDependencyCollector pomDependencyCollector;

    @Before
    public void setUpContext() throws Exception {
        pomDependencyCollector = new PomDependencyCollectorImpl(logVerifier.getLogger(),
                new ReactorProjectStub(tempManager.newFolder(), "test"), getProvisioningAgent());
    }

    @Test
    public void testReactorRepositoryManagerServiceAvailability() throws Exception {
        subject = lookup(ReactorRepositoryManager.class);

        assertNotNull(subject);
    }

    @Test
    public void testReactorRepositoryManagerFacadeServiceAvailability() throws Exception {
        subject = lookup(ReactorRepositoryManager.class);

        assertNotNull(subject);
    }

}
