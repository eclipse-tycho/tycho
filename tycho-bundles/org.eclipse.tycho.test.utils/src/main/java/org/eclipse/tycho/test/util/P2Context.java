/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.junit.Rule;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;

public final class P2Context extends ExternalResource {
    private final TemporaryFolder tempManager;
    private IProvisioningAgent agent;

    public P2Context() {
        this.tempManager = new TemporaryFolder();
    }

    @Override
    protected void before() throws Throwable {
        tempManager.create();
    }

    @Override
    protected void after() {
        if (agent != null) {
            agent.stop();
        }
        tempManager.delete();
    }

    /**
     * Returns an instance of an {@link IProvisioningAgent}. If this instance acts as a JUnit
     * {@link Rule}, there is a separate instance for each test.
     */
    public IProvisioningAgent getAgent() throws ProvisionException {
        if (agent == null) {
            agent = Activator.createProvisioningAgent(tempManager.newFolder("p2agent").toURI());
        }
        return agent;
    }

    public <T> T getService(Class<T> type) throws ProvisionException {
        return type.cast(getAgent().getService(type.getCanonicalName()));
    }
}
