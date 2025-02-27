/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.surefire.osgibooter;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

class BundleClassLoader extends ClassLoader {
    private Collection<Bundle> bundles;
    private boolean debug;

    public BundleClassLoader(List<Bundle> bundles, boolean debug) {
        this.bundles = new LinkedHashSet<>(bundles);
        this.debug = debug;
        for (Bundle bundle : bundles) {
            Objects.requireNonNull(bundle, "Bundle can't be null");
            BundleWiring wiring = Objects.requireNonNull(bundle.adapt(BundleWiring.class),
                    "Bundle needs to have a wiring, is it not resolved?");
            if (debug) {
                System.out.println("Bundle " + bundle + " is wired to:");
            }
            for (BundleWire wired : wiring.getRequiredWires(null)) {
                Bundle provider = wired.getProvider().getBundle();
                if (this.bundles.add(provider)) {
                    if (debug) {
                        System.out.println("  - " + provider.getSymbolicName() + " (" + provider.getVersion() + ")");
                    }
                }
            }
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        for (Bundle bundle : bundles) {
            try {
                return bundle.loadClass(name);
            } catch (ClassNotFoundException e) {
            }
        }
        String message = name + " was not found in " + toString();
        if (debug) {
            System.out.println(message);
        }
        throw new ClassNotFoundException(message);
    }

    @Override
    protected URL findResource(String name) {
        for (Bundle bundle : bundles) {
            URL resource = bundle.getResource(name);
            if (resource != null) {
                return resource;
            }
        }
        if (debug) {
            System.out.println("RESOURCE " + name + " was not found in " + toString());
        }
        return null;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        List<URL> result = new ArrayList<>();
        for (Bundle bundle : bundles) {
            Enumeration<URL> resources = bundle.getResources(name);
            if (resources == null) {
                continue;
            }
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                result.add(url);
            }
        }
        if (debug && result.isEmpty()) {
            System.out.println("RESOURCES " + name + " where not found in " + toString());
        }
        return Collections.enumeration(result);
    }

    @Override
    public String toString() {
        return bundles.stream().map(b -> b.getSymbolicName() + " [" + b.getVersion() + "]")
                .collect(Collectors.joining(" or "));
    }

}
