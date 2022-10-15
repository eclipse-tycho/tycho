/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.p2.impl;

import java.net.URI;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
    public static final String PLUGIN_ID = "org.eclipse.tycho.p2.impl";

    private static Activator instance;

    private BundleContext context;

    private static ServiceTracker<IProvisioningAgent, IProvisioningAgent> serviceTracker;

    public Activator() {
        Activator.instance = this;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        serviceTracker = new ServiceTracker<>(context, IProvisioningAgent.class, null);
        serviceTracker.open();
    }

    public static IProvisioningAgent getProvisioningAgent() {
        if (serviceTracker == null) {
            return null;
        }
        IProvisioningAgent service = serviceTracker.getService();
        return service;
    }

    /**
     * @deprecated This method potentially creates multiple agent instances with the default
     *             location. This leads to concurrent file system access with undefined outcome.
     */
    @Deprecated
    public static IProvisioningAgent newProvisioningAgent() throws ProvisionException {
        BundleContext context = getContext();

        ServiceReference<IProvisioningAgentProvider> agentFactoryReference = context
                .getServiceReference(IProvisioningAgentProvider.class);
        IProvisioningAgentProvider agentFactory = context.getService(agentFactoryReference);
        try {
            return agentFactory.createAgent(null); // null == currently running system
        } finally {
            context.ungetService(agentFactoryReference);
        }
    }

    public static IProvisioningAgent createProvisioningAgent(final URI targetLocation) throws ProvisionException {
        if (targetLocation == null)
            throw new IllegalArgumentException("Creating the default agent is not supported");

        BundleContext context = getContext();
        ServiceReference<IProvisioningAgentProvider> agentFactoryReference = context
                .getServiceReference(IProvisioningAgentProvider.class);
        IProvisioningAgentProvider agentFactory = context.getService(agentFactoryReference);
        try {
            return agentFactory.createAgent(targetLocation);
        } finally {
            context.ungetService(agentFactoryReference);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }

    public static BundleContext getContext() {
        return instance.context;
    }
}
