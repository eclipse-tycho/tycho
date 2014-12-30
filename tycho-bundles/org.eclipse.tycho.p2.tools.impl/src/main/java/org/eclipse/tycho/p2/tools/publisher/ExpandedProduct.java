/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductContentType;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.tycho.core.shared.Interpolator;
import org.eclipse.tycho.core.shared.VersioningHelper;

@SuppressWarnings({ "restriction" })
class ExpandedProduct implements IProductDescriptor {

    private final IProductDescriptor defaults;

    private Interpolator interpolator;
    private String buildQualifier;

    private final String expandedVersion;
    private List<IVersionedId> expandedBundles = null;
    private List<IVersionedId> expandedFeatures = null;

    // TODO 428889 this information should come from the IProductDescriptor
    private Set<String> rootFeatures;

    public ExpandedProduct(IProductDescriptor originalProduct, String buildQualifier, Interpolator interpolator,
            Set<String> rootFeatures) {
        this.defaults = originalProduct;
        this.buildQualifier = buildQualifier;
        this.rootFeatures = rootFeatures;
        this.expandedVersion = VersioningHelper.expandQualifier(originalProduct.getVersion(), buildQualifier);
        this.interpolator = interpolator;
    }

    @Override
    public String getVersion() {
        return expandedVersion;
    }

    @Override
    public List<IVersionedId> getBundles(boolean includeFragments) {
        if (includeFragments == false) {
            // currently not needed -> omitted for simplicity
            throw new UnsupportedOperationException();
        }
        if (getProductContentType() == ProductContentType.FEATURES) {
            // don't expand versions if bundles are not included in the product
            // TODO why is this method called anyway?
            return defaults.getBundles(includeFragments);
        }

        if (expandedBundles == null) {
            expandedBundles = qualifyReferences(defaults.getBundles(includeFragments));
        }
        return expandedBundles;
    }

    @Override
    public List<IVersionedId> getFragments() {
        // currently not needed -> omitted for simplicity
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IVersionedId> getFeatures() {
        if (getProductContentType() == ProductContentType.BUNDLES) {
            // TODO add hasFeatures() method to avoid this method to be called in bundle-based products
            return defaults.getFeatures();
        }
        if (expandedFeatures == null) {
            expandedFeatures = qualifyReferences(filter(defaults.getFeatures(), rootFeatures));
        }
        return expandedFeatures;
    }

    private List<IVersionedId> filter(List<IVersionedId> source, Set<String> toRemove) {
        List<IVersionedId> result = new ArrayList<IVersionedId>();
        for (IVersionedId entry : source) {
            if (toRemove.contains(entry.getId())) {
                // remove
            } else {
                result.add(entry);
            }
        }
        return result;
    }

    // TODO 373817 look up matching versions from the target platform
    private List<IVersionedId> qualifyReferences(List<IVersionedId> references) {
        List<IVersionedId> result = new ArrayList<IVersionedId>(references.size());
        for (IVersionedId reference : references) {
            result.add(new VersionedId(reference.getId(), VersioningHelper.expandQualifier(reference.getVersion()
                    .toString(), buildQualifier)));
        }
        return result;
    }

    @Override
    public Map<String, String> getConfigurationProperties() {
        return expandVariables(defaults.getConfigurationProperties());
    }

    @Override
    public Map<String, String> getConfigurationProperties(String os, String arch) {
        return expandVariables(defaults.getConfigurationProperties(os, arch));
    }

    private Map<String, String> expandVariables(Map<String, String> originalMap) {
        Map<String, String> expandedMap = new LinkedHashMap<String, String>();
        for (Entry<String, String> entry : originalMap.entrySet()) {
            expandedMap.put(entry.getKey(), interpolator.interpolate(entry.getValue()));
        }
        return expandedMap;
    }

    // delegating methods

    @Override
    public String getLauncherName() {
        return defaults.getLauncherName();
    }

    @Override
    public String getConfigIniPath(String os) {
        return defaults.getConfigIniPath(os);
    }

    @Override
    public String getId() {
        return defaults.getId();
    }

    @Override
    public String getProductId() {
        return defaults.getProductId();
    }

    @Override
    public String getApplication() {
        return defaults.getApplication();
    }

    @Override
    public String getSplashLocation() {
        return defaults.getSplashLocation();
    }

    @Override
    public String getProductName() {
        return defaults.getProductName();
    }

    @Override
    public boolean useFeatures() {
        return defaults.useFeatures();
    }

    @Override
    public ProductContentType getProductContentType() {
        return defaults.getProductContentType();
    }

    @Override
    public String getVMArguments(String os) {
        return defaults.getVMArguments(os);
    }

    @Override
    public String getVMArguments(String os, String arch) {
        return defaults.getVMArguments(os, arch);
    }

    @Override
    public String getProgramArguments(String os) {
        return defaults.getProgramArguments(os);
    }

    @Override
    public String getProgramArguments(String os, String arch) {
        return defaults.getProgramArguments(os, arch);
    }

    @Override
    public String[] getIcons(String os) {
        return defaults.getIcons(os);
    }

    @Override
    public List<BundleInfo> getBundleInfos() {
        return defaults.getBundleInfos();
    }

    @Override
    public File getLocation() {
        return defaults.getLocation();
    }

    @Override
    public boolean includeLaunchers() {
        return defaults.includeLaunchers();
    }

    @Override
    public String getLicenseURL() {
        return defaults.getLicenseURL();
    }

    @Override
    public String getLicenseText() {
        return defaults.getLicenseText();
    }

    @Override
    public List<IRepositoryReference> getRepositoryEntries() {
        return defaults.getRepositoryEntries();
    }

}
