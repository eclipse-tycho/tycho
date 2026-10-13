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
package org.eclipse.tycho.p2resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;
import org.junit.jupiter.api.extension.ExtendWith;
import org.eclipse.tycho.test.util.TychoPlexusExtension;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.tycho.core.test.utils.ResourceUtil;
import org.eclipse.tycho.test.util.LogVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(TychoPlexusExtension.class)
public class RemoteAgentDisableP2MirrorsTest {

    @Inject
    protected PlexusContainer container;

    @TempDir
    Path tempManager;
    @RegisterExtension
    public final LogVerifier logVerifier = new LogVerifier();

    @BeforeEach
    public void setUp() throws Exception {
    }

    @Test
    @Disabled("can't be tested that way!")
    public void testDisableP2Mirrors() throws Exception {
        IProvisioningAgent agent = createRemoteAgent(true);
        IArtifactRepository repo = loadRepository(agent, ResourceUtil.resourceFile("p2-mirrors-disable").toURI());

        assertNull(repo.getProperty(IRepository.PROP_MIRRORS_URL));
    }

    @Test
    public void testWithoutDisableP2Mirrors() throws Exception {
        IProvisioningAgent agent = createRemoteAgent(false);
        IArtifactRepository repo = loadRepository(agent, ResourceUtil.resourceFile("p2-mirrors-disable").toURI());

        assertEquals("file://dummy/", repo.getProperty(IRepository.PROP_MIRRORS_URL));
    }

    private IProvisioningAgent createRemoteAgent(boolean disableMirrors) throws Exception {
        return container.lookup(IProvisioningAgent.class);
    }

    private static IArtifactRepository loadRepository(IProvisioningAgent agent, URI location)
            throws ProvisionException {
        IArtifactRepositoryManager repoManager = agent.getService(IArtifactRepositoryManager.class);
        return repoManager.loadRepository(location, null);
    }

}
