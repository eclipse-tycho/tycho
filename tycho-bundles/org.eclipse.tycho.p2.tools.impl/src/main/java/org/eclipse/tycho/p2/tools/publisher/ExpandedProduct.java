/*******************************************************************************
 * Copyright (c) 2015, 2019 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - Bug 546382: Launcher Icon path is not considered correctly
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductContentType;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.Interpolator;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.core.shared.MultiLineLogger;
import org.eclipse.tycho.core.shared.VersioningHelper;
import org.eclipse.tycho.p2.target.P2TargetPlatform;

@SuppressWarnings({ "restriction" })
class ExpandedProduct implements IProductDescriptor {

    private final IProductDescriptor defaults;

    private P2TargetPlatform targetPlatform;
    private Interpolator interpolator;

    private final String expandedVersion;
    private List<IVersionedId> expandedBundles = null;
    private List<IVersionedId> expandedFeatures = null;
    private List<IInstallableUnit> expandedRootFeatures = Collections.emptyList();

    private final MultiLineLogger logger;

    public ExpandedProduct(IProductDescriptor originalProduct, String buildQualifier, P2TargetPlatform targetPlatform,
            Interpolator interpolator, MavenLogger logger) {
        this.defaults = originalProduct;
        this.expandedVersion = VersioningHelper.expandQualifier(originalProduct.getVersion(), buildQualifier);
        this.targetPlatform = targetPlatform;
        this.interpolator = interpolator;
        this.logger = new MultiLineLogger(logger);

        expandVersions();
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

        return expandedBundles;
    }

    @Override
    public List<IVersionedId> getFragments() {
        // currently not needed -> omitted for simplicity
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IVersionedId> getFeatures() {
        return getFeatures(INCLUDED_FEATURES);
    }

    @Override
    public List<IVersionedId> getFeatures(int options) {
        if (options == INCLUDED_FEATURES) {
            return expandedFeatures;
        } else {
            // currently not needed by the publisher action -> omitted for simplicity
            throw new UnsupportedOperationException();
        }
    }

    public List<IInstallableUnit> getRootFeatures() {
        return expandedRootFeatures;
    }

    private void expandVersions() {
        ProductContentType contentType = getProductContentType();
        ProductVersionExpansionRun resolver = new ProductVersionExpansionRun(targetPlatform, getLocation());
        if (contentType != ProductContentType.FEATURES) {
            expandedBundles = resolver.resolveReferences("plugin", ArtifactType.TYPE_ECLIPSE_PLUGIN,
                    defaults.getBundles(true));
        }
        if (contentType != ProductContentType.BUNDLES) {
            expandedFeatures = resolver.resolveReferences("feature", ArtifactType.TYPE_ECLIPSE_FEATURE,
                    defaults.getFeatures(INCLUDED_FEATURES));
            expandedRootFeatures = resolver.resolveReferencesToIUs("feature", ArtifactType.TYPE_ECLIPSE_FEATURE,
                    defaults.getFeatures(ROOT_FEATURES));
        }
        resolver.reportErrors(logger);
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
        Map<String, String> expandedMap = new LinkedHashMap<>();
        for (Entry<String, String> entry : originalMap.entrySet()) {
            expandedMap.put(entry.getKey(), interpolator.interpolate(entry.getValue()));
        }
        return expandedMap;
    }

    // delegating methods

    @Override
    public boolean hasBundles(boolean includeFragments) {
        // don't need to expand versions for this check
        return defaults.hasBundles(includeFragments);
    }

    @Override
    public boolean hasFeatures() {
        // don't need to expand versions for this check
        return defaults.hasFeatures();
    }

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
        String[] icons = defaults.getIcons(os);
        logger.debug("Getting the following icon paths from defaults: " + Arrays.toString(icons));
        for (int i = 0; i < icons.length; i++) {
            icons[i] = guessRealIconPath(icons[i]);

        }
        return icons;
    }

    private String guessRealIconPath(String path) {
        File file = new File(path);
        if (!file.exists()) {
            try {
                File productFolder = defaults.getLocation().getParentFile();
                String productPath = productFolder.getCanonicalPath();
                String iconPath = file.getCanonicalPath();
                if (iconPath.startsWith(productPath)) {
                    String rawPath = iconPath.substring(productPath.length());
                    if (rawPath.startsWith("/") || rawPath.startsWith(File.separator)) {
                        //remove the first slash (indicates the root of the workspace)
                        rawPath = rawPath.substring(1);
                    }
                    File parentDirectory = productFolder.getParentFile();
                    if (parentDirectory != null) {
                        File guessedFile = new File(parentDirectory, rawPath);
                        String absolutePath = guessedFile.getAbsolutePath();
                        logger.debug("raw path is " + rawPath + ", guessed path is " + absolutePath);
                        if (guessedFile.exists()) {
                            return absolutePath;
                        }
                    }
                }
                logger.warn("Icon path " + path
                        + " does not exist and can't be determined by tycho, make sure that either the icon path is relative to the product file, or denotes a folder in the parent path of the product! (current product path is "
                        + productPath + ")");
            } catch (IOException e) {
                logger.warn("can't guess icon path because of I/O problem", e);
            }
        }
        return path;
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

    @Override
    public String getVM(String os) {
        return defaults.getVM(os);
    }

}
