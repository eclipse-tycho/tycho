/*******************************************************************************
 * Copyright (c) 2008, 2014 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.publisher;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductContentType;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.AdviceFileAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;

@SuppressWarnings("restriction")
public class ProductDependenciesAction extends AbstractDependenciesAction {
    private final IProductDescriptor product;

    public ProductDependenciesAction(IProductDescriptor product) {
        this.product = product;
    }

    @Override
    protected Version getVersion() {
        return Version.create(product.getVersion());
    }

    @Override
    protected String getId() {
        return product.getId();
    }

    @Override
    protected Set<IRequirement> getRequiredCapabilities() {
        Set<IRequirement> required = new LinkedHashSet<>();

        ProductContentType type = product.getProductContentType();
        if (type == ProductContentType.FEATURES || type == ProductContentType.MIXED) {
            for (IVersionedId feature : product.getFeatures()) {
                String id = feature.getId() + FEATURE_GROUP_IU_SUFFIX; //$NON-NLS-1$
                Version version = feature.getVersion();

                addRequiredCapability(required, id, version, null, false);
            }
        }
        if (type == ProductContentType.BUNDLES || type == ProductContentType.MIXED) {
            for (FeatureEntry plugin : ((ProductFile) product).getProductEntries()) {
                addRequiredCapability(required, plugin.getId(), Version.parseVersion(plugin.getVersion()), null, true);
            }
        }

        if (product.includeLaunchers()) {
            addRequiredCapability(required, "org.eclipse.equinox.executable.feature.group", null, null, false);
        }
        return required;
    }

    @Override
    protected void addPublisherAdvice(IPublisherInfo publisherInfo) {
        // see org.eclipse.equinox.p2.publisher.eclipse.ProductAction.createAdviceFileAdvice()

        File productFileLocation = product.getLocation();
        if (productFileLocation == null) {
            return;
        }

        String id = product.getId();
        Version parseVersion = Version.parseVersion(product.getVersion());
        IPath basePath = new Path(productFileLocation.getParent());

        // must match org.eclipse.tycho.plugins.p2.publisher.PublishProductMojo.getSourceP2InfFile(File)
        final String productFileName = productFileLocation.getName();
        final String p2infFilename = productFileName.substring(0, productFileName.length() - ".product".length())
                + ".p2.inf";

        AdviceFileAdvice advice = new AdviceFileAdvice(id, parseVersion, basePath, new Path(p2infFilename));
        if (advice.containsAdvice()) {
            publisherInfo.addAdvice(advice);
        }
    }

    @Override
    protected void addProperties(InstallableUnitDescription iud) {
        iud.setProperty(InstallableUnitDescription.PROP_TYPE_PRODUCT, Boolean.toString(true));
    }
}
