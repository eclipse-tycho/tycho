/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

public class Activator implements BundleActivator {
    public static final String PLUGIN_ID = "org.eclipse.tycho.p2.impl";

    private static Activator instance;

    private BundleContext context;

    public Activator() {
        Activator.instance = this;
    }

    public void start(BundleContext context) throws Exception {
        this.context = context;
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

    public void stop(BundleContext context) throws Exception {
    }

    public static BundleContext getContext() {
        return instance.context;
    }
}
