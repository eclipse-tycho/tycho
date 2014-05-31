/*******************************************************************************
 * Copyright (c) 2008, 2014 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.surefire.osgibooter;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

public class Activator implements BundleActivator {

    public static final String PLUGIN_ID = "org.eclipse.tycho.surefire.osgibooter";
    private static PlatformAdmin platformAdmin;
    private static LogService log;

    public Activator() {
    }

    public void start(BundleContext context) throws Exception {
        ServiceReference platformAdminRef = context.getServiceReference(PlatformAdmin.class.getName());
        if (platformAdminRef != null) {
            platformAdmin = (PlatformAdmin) context.getService(platformAdminRef);
        }
        ServiceReference logServiceRef = context.getServiceReference(LogService.class.getName());
        if (logServiceRef != null) {
            log = (LogService) context.getService(logServiceRef);
        }
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

    public static Set<ResolverError> getResolutionErrors(Bundle bundle) {
        Set<ResolverError> errors = new LinkedHashSet<ResolverError>();
        try {
            if (platformAdmin == null) {
                System.err
                        .println("Could not compute diagnostic information for the test bundle resolution problems - PlatformAdmin service is not available");
                return errors;
            }
            State state = platformAdmin.getState(false /* mutable */);
            if (state == null) {
                System.err.println("Resolver state is null");
                return errors;
            }
            BundleDescription description = state.getBundle(bundle.getBundleId());
            if (description == null) {
                System.err.println("Could not determine BundleDescription for " + bundle.toString());
            }
            getRelevantErrors(state, errors, description);
        } catch (RuntimeException e) {
            System.err.println("Error while computing diagnostic information for the test bundle resolution problems");
            e.printStackTrace();
        }
        return errors;
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

    public static LogService getLog() {
        return log;
    }
}
