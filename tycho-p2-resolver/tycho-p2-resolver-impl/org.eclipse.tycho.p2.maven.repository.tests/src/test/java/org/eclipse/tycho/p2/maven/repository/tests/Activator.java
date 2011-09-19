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
package org.eclipse.tycho.p2.maven.repository.tests;

import java.net.URI;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class Activator implements BundleActivator {

    private static BundleContext context;

    public void start(BundleContext context) throws Exception {
        Activator.context = context;
    }

    public void stop(BundleContext context) throws Exception {
        Activator.context = null;
    }

    public static BundleContext getContext() {
        return context;
    }

    public static IProvisioningAgent createProvisioningAgent(final URI targetLocation) throws ProvisionException {
        ServiceReference serviceReference = context.getServiceReference(IProvisioningAgentProvider.SERVICE_NAME);
        IProvisioningAgentProvider agentFactory = (IProvisioningAgentProvider) context.getService(serviceReference);
        try {
            return agentFactory.createAgent(targetLocation);
        } finally {
            context.ungetService(serviceReference);
        }
    }
}
