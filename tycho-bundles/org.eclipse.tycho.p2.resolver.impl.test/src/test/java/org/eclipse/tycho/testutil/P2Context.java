/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP SE and others.
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

import java.io.IOException;

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
            try {
                agent = org.eclipse.tycho.p2.impl.Activator
                        .createProvisioningAgent(tempManager.newFolder("p2agent").toURI());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return agent;
    }

    public <T> T getService(Class<T> type) throws ProvisionException {
        return type.cast(getAgent().getService(type.getCanonicalName()));
    }
}
