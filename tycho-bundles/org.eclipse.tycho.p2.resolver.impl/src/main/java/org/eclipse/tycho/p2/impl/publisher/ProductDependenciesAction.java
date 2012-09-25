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
package org.eclipse.tycho.p2.impl.publisher;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.AdviceFileAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.eclipse.osgi.service.environment.Constants;

@SuppressWarnings("restriction")
public class ProductDependenciesAction extends AbstractDependenciesAction {
    private final IProductDescriptor product;

    private final List<Map<String, String>> environments;

    public ProductDependenciesAction(IProductDescriptor product, List<Map<String, String>> environments) {
        this.product = product;
        this.environments = environments;
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
        Set<IRequirement> required = new LinkedHashSet<IRequirement>();

        if (product.useFeatures()) {
            for (IVersionedId feature : (List<IVersionedId>) product.getFeatures()) {
                String id = feature.getId() + FEATURE_GROUP_IU_SUFFIX; //$NON-NLS-1$
                Version version = feature.getVersion();

                addRequiredCapability(required, id, version, null, false);
            }
        } else {
            for (FeatureEntry plugin : ((ProductFile) product).getProductEntries()) {
                addRequiredCapability(required, plugin.getId(), Version.parseVersion(plugin.getVersion()), null, true);
            }
        }

        // these are implicitly required, see
        // See also org.eclipse.tycho.osgitools.AbstractArtifactDependencyWalker.traverseProduct
        addRequiredCapability(required, "org.eclipse.equinox.launcher", null, null, false);

        if (product.includeLaunchers()) {
            addRequiredCapability(required, "org.eclipse.equinox.executable.feature.group", null, null, false);

            if (environments != null) {
                for (Map<String, String> env : environments) {
                    addNativeRequirements(required, env.get(OSGI_OS), env.get(OSGI_WS), env.get(OSGI_ARCH));
                }
            }
        }
        return required;
    }

    private void addNativeRequirements(Set<IRequirement> required, String os, String ws, String arch) {
        String filter = getFilter(os, ws, arch);

        if (Constants.OS_MACOSX.equals(os)) {
            // macosx is twisted
            if (Constants.ARCH_X86.equals(arch)) {
                addRequiredCapability(required, "org.eclipse.equinox.launcher." + ws + "." + os, null, filter, false);
                return;
            }
        }

        addRequiredCapability(required, "org.eclipse.equinox.launcher." + ws + "." + os + "." + arch, null, filter,
                false);
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
        final int indexOfExtension = productFileLocation.getName().indexOf(".product");
        final String p2infFilename = productFileLocation.getName().substring(0, indexOfExtension) + ".p2.inf";

        AdviceFileAdvice advice = new AdviceFileAdvice(id, parseVersion, basePath, new Path(p2infFilename));
        if (advice.containsAdvice()) {
            publisherInfo.addAdvice(advice);
        }
    }
}
