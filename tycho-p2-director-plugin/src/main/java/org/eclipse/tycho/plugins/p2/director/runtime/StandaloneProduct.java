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
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director.runtime;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductContentType;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.publisher.eclipse.IMacOsBundleUrlType;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

class StandaloneProduct implements IProductDescriptor {

    private final IProductDescriptor delegate;

    private List<IVersionedId> expandedBundles = new ArrayList<>();

    public StandaloneProduct(File productFile, IMetadataRepository repository) throws CoreException {
        this.delegate = new ProductFile(productFile.getAbsolutePath(), null);
        List<IVersionedId> bundles = delegate.getBundles();
        for (IVersionedId bundle : bundles) {
            Iterator<IInstallableUnit> iterator = repository.query(QueryUtil.createIUQuery(bundle.getId()), null)
                    .iterator();
            if (iterator.hasNext()) {
                IInstallableUnit next = iterator.next();
                expandedBundles.add(new VersionedId(next.getId(), next.getVersion()));
            } else {
                expandedBundles.add(bundle);
            }
        }
    }

    @Override
    public String getVersion() {
        return delegate.getVersion();
    }

    @Override
    public List<IVersionedId> getBundles() {
        return expandedBundles;
    }

    @Override
    public List<IVersionedId> getFeatures() {
        return getFeatures(INCLUDED_FEATURES);
    }

    @Override
    public List<IVersionedId> getFeatures(int options) {
        return delegate.getFeatures(options);
    }

    @Override
    public Map<String, String> getConfigurationProperties() {
        return delegate.getConfigurationProperties();
    }

    @Override
    public Map<String, String> getConfigurationProperties(String os, String arch) {
        return delegate.getConfigurationProperties(os, arch);
    }

    @Override
    public boolean hasBundles() {
        return delegate.hasBundles();
    }

    @Override
    public boolean hasFeatures() {
        return delegate.hasFeatures();
    }

    @Override
    public String getLauncherName() {
        return delegate.getLauncherName();
    }

    @Override
    public String getConfigIniPath(String os) {
        return delegate.getConfigIniPath(os);
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public String getProductId() {
        return delegate.getProductId();
    }

    @Override
    public String getApplication() {
        return delegate.getApplication();
    }

    @Override
    public String getSplashLocation() {
        return delegate.getSplashLocation();
    }

    @Override
    public String getProductName() {
        return delegate.getProductName();
    }

    @Override
    public boolean useFeatures() {
        return delegate.useFeatures();
    }

    @Override
    public ProductContentType getProductContentType() {
        return delegate.getProductContentType();
    }

    @Override
    public String getVMArguments(String os) {
        return delegate.getVMArguments(os);
    }

    @Override
    public String getVMArguments(String os, String arch) {
        return delegate.getVMArguments(os, arch);
    }

    @Override
    public String getProgramArguments(String os) {
        return delegate.getProgramArguments(os);
    }

    @Override
    public String getProgramArguments(String os, String arch) {
        return delegate.getProgramArguments(os, arch);
    }

    @Override
    public String[] getIcons(String os) {
        return delegate.getIcons(os);
    }

    @Override
    public List<BundleInfo> getBundleInfos() {
        return delegate.getBundleInfos();
    }

    @Override
    public File getLocation() {
        return delegate.getLocation();
    }

    @Override
    public boolean includeLaunchers() {
        return delegate.includeLaunchers();
    }

    @Override
    public String getLicenseURL() {
        return delegate.getLicenseURL();
    }

    @Override
    public String getLicenseText() {
        return delegate.getLicenseText();
    }

    @Override
    public List<IRepositoryReference> getRepositoryEntries() {
        return delegate.getRepositoryEntries();
    }

    @Override
    public String getVM(String os) {
        return delegate.getVM(os);
    }

    @Override
    public List<IMacOsBundleUrlType> getMacOsBundleUrlTypes() {
        return delegate.getMacOsBundleUrlTypes();
    }

}
