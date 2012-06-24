/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.module;

import java.io.File;
import java.io.IOException;

import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.ReactorProjectCoordinates;
import org.eclipse.tycho.repository.publishing.PublishingRepository;
import org.eclipse.tycho.repository.registry.ReactorRepositoryManager;

public class ReactorRepositoryManagerImpl implements ReactorRepositoryManager {

    private IProvisioningAgentProvider agentFactory;
    private File agentDir;
    private IProvisioningAgent agent;

    public void bindProvisioningAgentFactory(IProvisioningAgentProvider agentFactory) {
        this.agentFactory = agentFactory;
    }

    public void activateManager() throws IOException, ProvisionException {
        agentDir = createTempDir("tycho_reactor_agent");
        agent = agentFactory.createAgent(agentDir.toURI());
    }

    public void deactivateManager() {
        agent.stop();
        FileUtils.deleteAll(agentDir);
    }

    // TODO hide?
    public IProvisioningAgent getAgent() {
        return agent;
    }

    public PublishingRepository getPublishingRepository(ReactorProjectCoordinates project) {
        return new PublishingRepositoryImpl(agent, project);
    }

    private static File createTempDir(String prefix) throws IOException {
        File tempFile = File.createTempFile(prefix, "");
        tempFile.delete();
        tempFile.mkdirs();
        if (!tempFile.isDirectory()) {
            throw new IOException("Failed to create temporary directory: " + tempFile);
        }
        return tempFile;
    }
}
