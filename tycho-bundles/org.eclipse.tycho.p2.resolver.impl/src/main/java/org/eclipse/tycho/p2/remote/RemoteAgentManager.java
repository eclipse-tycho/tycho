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

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.core.facade.MavenContext;

/**
 * Manager for {@link RemoteAgent} instances used to access remote p2 repositories. The instance are
 * shared within a reactor because they cache the loaded p2 repositories.
 */
public class RemoteAgentManager {

    private MavenContext mavenContext;

    // TODO stop during bundle.stop?
    private IProvisioningAgent cachedAgent;

    public RemoteAgentManager(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
    }

    // constructor for DS
    public RemoteAgentManager() {
    }

    public synchronized IProvisioningAgent getProvisioningAgent() throws ProvisionException {
        if (cachedAgent == null) {
            cachedAgent = new RemoteAgent(mavenContext);
        }
        return cachedAgent;
    }

    public void setMavenContext(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
    }
}
