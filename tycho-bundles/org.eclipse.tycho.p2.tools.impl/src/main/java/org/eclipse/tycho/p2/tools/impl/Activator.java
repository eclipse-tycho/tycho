/*******************************************************************************
 * Copyright (c) 2010, 2020 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *     Christoph Läubrich - improve service handling
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.impl;

import java.util.concurrent.TimeUnit;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.BuildDirectory;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

    private static ServiceTracker<IProvisioningAgentProvider, IProvisioningAgentProvider> tracker;
    private static BundleContext context;

    @Override
    public void start(BundleContext context) throws Exception {
        synchronized (Activator.class) {
            Activator.context = context;
            tracker = new ServiceTracker<>(context, IProvisioningAgentProvider.SERVICE_NAME, null);
            tracker.open();
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        synchronized (Activator.class) {
            tracker.close();
            Activator.context = null;
        }
    }

    public static IProvisioningAgent createProvisioningAgent(final BuildDirectory targetDirectory)
            throws FacadeException {
        ServiceTracker<IProvisioningAgentProvider, IProvisioningAgentProvider> serviceTracker;
        synchronized (Activator.class) {
            serviceTracker = tracker;
            if (serviceTracker == null) {
                throw new FacadeException("not started");
            }
        }
        try {
            IProvisioningAgentProvider agentFactory = serviceTracker.waitForService(TimeUnit.SECONDS.toMillis(10));
            if (agentFactory == null) {
                throw new FacadeException("not started");
            }
            try {
                return agentFactory.createAgent(targetDirectory.getP2AgentDirectory().toURI());
            } catch (ProvisionException e) {
                throw new FacadeException(e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FacadeException(e);
        }
    }

    public static BundleContext getContext() {
        synchronized (Activator.class) {
            return context;
        }
    }
}
