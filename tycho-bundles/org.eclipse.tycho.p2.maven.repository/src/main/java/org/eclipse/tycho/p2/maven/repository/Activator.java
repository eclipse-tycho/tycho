/*******************************************************************************
 * Copyright (c) 2008, 2013 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.maven.repository;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
    private static BundleContext context;

    private static ServiceTracker<IProvisioningAgent, IProvisioningAgent> serviceTracker;

    @Override
    public void start(BundleContext context) throws Exception {
        Activator.context = context;
        serviceTracker = new ServiceTracker<>(context, IProvisioningAgent.class, null);
        serviceTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        Activator.context = null;
    }

    public static BundleContext getContext() {
        return context;
    }

    // TODO repositories should not make assumptions on the agent they are loaded by (see callers)
    public static IProvisioningAgent getProvisioningAgent() {
        if (serviceTracker == null) {
            return null;
        }
        IProvisioningAgent service = serviceTracker.getService();
        return service;
    }
}
