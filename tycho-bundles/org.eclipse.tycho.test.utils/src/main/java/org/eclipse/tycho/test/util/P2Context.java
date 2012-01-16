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
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;

public final class P2Context extends ExternalResource {
    TemporaryFolder tempFolder = new TemporaryFolder();
    private IProvisioningAgent agent;

    @Override
    protected void before() throws Throwable {
        tempFolder.create();
        agent = Activator.createProvisioningAgent(tempFolder.newFolder("p2agent").toURI());

    }

    @Override
    protected void after() {
        agent.stop();
        tempFolder.delete();
    }

    public IProvisioningAgent getAgent() {
        return agent;
    }
}
