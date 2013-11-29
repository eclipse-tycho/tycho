/*******************************************************************************
 * Copyright (c) 2008, 2013 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Red Hat Inc.  - Remove ref to PlatformAdmin
 *******************************************************************************/
package org.eclipse.tycho.surefire.osgibooter;

import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    public static final String PLUGIN_ID = "org.eclipse.tycho.surefire.osgibooter";

    public Activator() {
    }

    public void start(BundleContext context) throws Exception {
    }

    public void stop(BundleContext context) throws Exception {
    }

    public static Bundle getBundle(String symbolicName) {
        Bundle bundle = Platform.getBundle(symbolicName);
        if (bundle == null) {
            return null;
        }

        if (Platform.isFragment(bundle)) {
            Bundle[] hosts = Platform.getHosts(bundle);
            if (hosts != null && hosts.length > 0) {
                // TODO do we care about multiple hosts???
                return hosts[0];
            }
            throw new IllegalArgumentException("Fragment bundle is not attached to a host " + symbolicName);
        }

        return bundle;
    }

    private static void getRelevantErrors(State state, Set<ResolverError> errors, BundleDescription bundle) {
        ResolverError[] bundleErrors = state.getResolverErrors(bundle);
        for (int j = 0; j < bundleErrors.length; j++) {
            ResolverError error = bundleErrors[j];
            errors.add(error);

            VersionConstraint constraint = error.getUnsatisfiedConstraint();
            if (constraint instanceof BundleSpecification || constraint instanceof HostSpecification) {
                BundleDescription[] requiredBundles = state.getBundles(constraint.getName());
                for (int i = 0; i < requiredBundles.length; i++) {
                    getRelevantErrors(state, errors, requiredBundles[i]);
                }
            }
        }
    }

}
