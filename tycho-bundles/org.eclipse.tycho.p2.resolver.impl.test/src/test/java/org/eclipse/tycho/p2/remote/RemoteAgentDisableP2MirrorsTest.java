/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.remote;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.net.URI;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.tycho.core.shared.MockMavenContext;
import org.eclipse.tycho.p2.impl.test.ResourceUtil;
import org.eclipse.tycho.test.util.LogVerifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RemoteAgentDisableP2MirrorsTest {

    @Rule
    public final TemporaryFolder tempManager = new TemporaryFolder();
    @Rule
    public final LogVerifier logVerifier = new LogVerifier();

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testDisableP2Mirrors() throws Exception {
        IProvisioningAgent agent = createRemoteAgent(true);
        IArtifactRepository repo = loadRepository(agent, ResourceUtil.resourceFile("p2-mirrors-disable").toURI());

        assertThat(repo.getProperty(IRepository.PROP_MIRRORS_URL), is(nullValue()));
    }

    @Test
    public void testWithoutDisableP2Mirrors() throws Exception {
        IProvisioningAgent agent = createRemoteAgent(false);
        IArtifactRepository repo = loadRepository(agent, ResourceUtil.resourceFile("p2-mirrors-disable").toURI());

        assertThat(repo.getProperty(IRepository.PROP_MIRRORS_URL), is("file://dummy/"));
    }

    private IProvisioningAgent createRemoteAgent(boolean disableMirrors) throws Exception {
        File localRepo = tempManager.newFolder("localRepo");
        return new RemoteAgent(new MockMavenContext(localRepo, logVerifier.getLogger()), disableMirrors);
    }

    private static IArtifactRepository loadRepository(IProvisioningAgent agent, URI location)
            throws ProvisionException {
        IArtifactRepositoryManager repoManager = agent.getService(IArtifactRepositoryManager.class);
        return repoManager.loadRepository(location, null);
    }

}
