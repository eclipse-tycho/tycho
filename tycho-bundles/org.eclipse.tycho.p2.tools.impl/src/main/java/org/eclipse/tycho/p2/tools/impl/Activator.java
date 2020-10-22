/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.impl;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class Activator implements BundleActivator {
    private static BundleContext context;

    @Override
    public void start(BundleContext context) throws Exception {
        Activator.context = context;
    }

    @Override
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

    public static BundleContext getContext() {
        return context;
    }
}
