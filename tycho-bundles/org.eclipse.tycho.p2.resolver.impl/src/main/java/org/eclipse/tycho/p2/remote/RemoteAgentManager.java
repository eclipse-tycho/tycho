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
package org.eclipse.tycho.p2.remote;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.core.facade.MavenContext;

/**
 * Manager for {@link RemoteAgent} instances used to access remote p2 repositories. The instance are
 * shared within a reactor because they cache the loaded p2 repositories.
 */
public class RemoteAgentManager {

    private MavenContext mavenContext;

    /**
     * Cached provisioning agent instances, indexed by the disableMirrors parameter.
     */
    // TODO stop when this service is stopped?
    private Map<Boolean, IProvisioningAgent> cachedAgents = new HashMap<Boolean, IProvisioningAgent>(2);

    public RemoteAgentManager(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
    }

    // constructor for DS
    public RemoteAgentManager() {
    }

    public synchronized IProvisioningAgent getProvisioningAgent(boolean disableP2Mirrors) throws ProvisionException {
        Boolean key = Boolean.valueOf(disableP2Mirrors);

        IProvisioningAgent agent = cachedAgents.get(key);
        if (agent == null) {
            agent = new RemoteAgent(mavenContext, disableP2Mirrors);
            cachedAgents.put(key, agent);

            if (cachedAgents.size() > 1) {
                String message = "The target platform configuration disableP2Mirrors=" + disableP2Mirrors
                        + " in this project is different from the configuration in other projects"
                        + " in the same reactor."
                        + " This may lead to redundant loading of p2 repositories and hence a slower build.";
                mavenContext.getLogger().warn(message);
            }
        }
        return agent;
    }

    public void setMavenContext(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
    }
}
