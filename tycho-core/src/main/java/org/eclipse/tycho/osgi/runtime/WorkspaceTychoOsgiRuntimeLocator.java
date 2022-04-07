/*******************************************************************************
 * Copyright (c) 2012, 2020 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.runtime;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.Artifact;
import org.eclipse.sisu.equinox.embedder.EquinoxRuntimeLocator.EquinoxRuntimeDescription;
import org.eclipse.tycho.dev.DevBundleInfo;
import org.eclipse.tycho.dev.DevWorkspaceResolver;
import org.eclipse.tycho.model.BundleConfiguration;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.model.ProductConfiguration;

class WorkspaceTychoOsgiRuntimeLocator {

    private final DevWorkspaceResolver workspaceState;

    private final File stateLocation;

    /**
     * dev.properties entries of bundles resolved by this resolver instance.
     */
    private Properties deventries = new Properties();

    private WorkspaceTychoOsgiRuntimeLocator(DevWorkspaceResolver workspaceState) {
        this.workspaceState = workspaceState;
        stateLocation = workspaceState.getStateLocation();
    }

    public boolean addProduct(EquinoxRuntimeDescription result, Artifact pom) throws MavenExecutionException {
        ProductConfiguration product;
        try {
            product = ProductConfiguration
                    .read(new File(pom.getFile().getParentFile(), pom.getArtifactId() + ".product"));
        } catch (IOException e) {
            return false;
        }

        // the above fails with IOException if .product file is not available or can't be read
        // we get here only when we have valid product instance

        Set<String> missing = new LinkedHashSet<>();
        for (PluginRef pluginRef : product.getPlugins()) {
            DevBundleInfo bundleInfo = workspaceState.getBundleInfo(pluginRef.getId(), pluginRef.getVersion());
            if (bundleInfo != null) {
                addBundle(result, bundleInfo);
            } else {
                missing.add(pluginRef.toString());
            }
        }

        if (!missing.isEmpty()) {
            throw new MavenExecutionException(
                    "Inconsistent m2e-tycho workspace state, missing bundles: " + missing.toString(), (Throwable) null);
        }

        Map<String, BundleConfiguration> bundleConfigurations = product.getPluginConfiguration();
        if (bundleConfigurations != null) {
            for (BundleConfiguration bundleConfiguration : bundleConfigurations.values()) {
                result.addBundleStartLevel(bundleConfiguration.getId(), bundleConfiguration.getStartLevel(),
                        bundleConfiguration.isAutoStart());
            }
        }

        return true;
    }

    public static WorkspaceTychoOsgiRuntimeLocator getResolver(DevWorkspaceResolver workspaceResolver) {
        if (workspaceResolver.getStateLocation() == null) {
            return null;
        }

        return new WorkspaceTychoOsgiRuntimeLocator(workspaceResolver);
    }

    public boolean addBundle(EquinoxRuntimeDescription result, Artifact pom) {
        DevBundleInfo bundleInfo = workspaceState.getBundleInfo(pom.getFile().getParentFile());
        if (bundleInfo == null) {
            return false;
        }
        addBundle(result, bundleInfo);
        return true;
    }

    private void addBundle(EquinoxRuntimeDescription result, DevBundleInfo bundleInfo) {
        result.addBundle(bundleInfo.getLocation());
        if (bundleInfo.getDevEntries() != null) {
            this.deventries.put(bundleInfo.getSymbolicName(), bundleInfo.getDevEntries());
        }
    }

    public void addPlatformProperties(EquinoxRuntimeDescription result) throws MavenExecutionException {
        result.addPlatformProperty("osgi.install.area", stateLocation.getAbsolutePath());

        File devproperties = new File(stateLocation, "dev.properties");
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(devproperties))) {
            deventries.store(os, null);
            result.addPlatformProperty("osgi.dev", devproperties.toURI().toURL().toExternalForm());
        } catch (IOException e) {
            throw new MavenExecutionException("Could not write dev.properties", e);
        }
    }
}
