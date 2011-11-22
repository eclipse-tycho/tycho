/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.impl;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.p2.tools.BuildOutputDirectory;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class Activator implements BundleActivator {
    private static BundleContext context;

    public void start(BundleContext context) throws Exception {
        Activator.context = context;
    }

    public void stop(BundleContext context) throws Exception {
    }

    public static IProvisioningAgent createProvisioningAgent(final BuildOutputDirectory targetDirectory)
            throws FacadeException {
        ServiceReference<?> serviceReference = context.getServiceReference(IProvisioningAgentProvider.SERVICE_NAME);
        IProvisioningAgentProvider agentFactory = (IProvisioningAgentProvider) context.getService(serviceReference);
        try {
            return agentFactory.createAgent(targetDirectory.getChild("p2agent").toURI());
        } catch (ProvisionException e) {
            throw new FacadeException(e);
        } finally {
            context.ungetService(serviceReference);
        }
    }
}
