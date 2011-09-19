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

    public static IProvisioningAgent newProvisioningAgent() throws ProvisionException {
        BundleContext context = getContext();

        ServiceReference providerRef = context.getServiceReference(IProvisioningAgentProvider.SERVICE_NAME);
        IProvisioningAgentProvider provider = (IProvisioningAgentProvider) context.getService(providerRef);
        try {
            return provider.createAgent(null); // null == currently running system
        } finally {
            context.ungetService(providerRef);
        }
    }

    public void stop(BundleContext context) throws Exception {
    }

    public static BundleContext getContext() {
        return instance.context;
    }
}
