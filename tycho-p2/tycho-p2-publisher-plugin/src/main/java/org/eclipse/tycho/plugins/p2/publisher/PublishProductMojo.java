/*******************************************************************************
 * Copyright (c) 2010, 2015 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.publisher;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.maven.TychoInterpolator;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.core.shared.Interpolator;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.locking.facade.FileLocker;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.p2.tools.publisher.facade.PublishProductTool;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherServiceFactory;

/**
 * <p>
 * Publishes all product definitions files (<tt>*.product</tt>) that are present in the root of the
 * project.
 * </p>
 * 
 * @see http://wiki.eclipse.org/Equinox/p2/Publisher
 */
@Mojo(name = "publish-products", defaultPhase = LifecyclePhase.PACKAGE)
public final class PublishProductMojo extends AbstractPublishMojo {

    /**
     * <p>
     * The name of the p2 installation flavor to create. De facto, this parameter is set to
     * "tooling" in all uses of p2.
     * </p>
     * 
     * @deprecated This parameter has no useful effect and may be removed in a future version of
     *             Tycho.
     */
    @Parameter(defaultValue = "tooling")
    @Deprecated
    private String flavor;

    @Component(role = UnArchiver.class, hint = "zip")
    private UnArchiver deflater;

    @Component
    private FileLockService fileLockService;

    @Override
    protected Collection<DependencySeed> publishContent(PublisherServiceFactory publisherServiceFactory)
            throws MojoExecutionException, MojoFailureException {
        Interpolator interpolator = new TychoInterpolator(getSession(), getProject());
        PublishProductTool publisher = publisherServiceFactory.createProductPublisher(getReactorProject(),
                getEnvironments(), getQualifier(), interpolator);

        List<DependencySeed> seeds = new ArrayList<DependencySeed>();
        for (File productFile : getEclipseRepositoryProject().getProductFiles(getProject())) {
            try {
                ProductConfiguration productConfiguration = ProductConfiguration.read(productFile);
                if (isEmpty(productConfiguration.getId())) {
                    throw new MojoExecutionException("The product file " + productFile.getName()
                            + " does not contain the mandatory attribute 'uid'");
                } else if (isEmpty(productConfiguration.getVersion())) {
                    throw new MojoExecutionException("The product file " + productFile.getName()
                            + " does not contain the mandatory attribute 'version'");
                }

                Set<String> rootFeatures = extractRootFeatures(productConfiguration, seeds);
                seeds.addAll(publisher.publishProduct(productFile, rootFeatures,
                        productConfiguration.includeLaunchers() ? getExpandedLauncherBinaries() : null, flavor));
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "I/O exception while writing product definition or copying launcher icons", e);
            }
        }
        return seeds;
    }

    static Set<String> extractRootFeatures(ProductConfiguration product, List<DependencySeed> seeds) {
        final String productId = product.getId();
        Set<String> rootFeatures = new HashSet<String>();

        // add root features as special dependency seed which are marked as "add-on" for the product
        DependencySeed.Filter filter = new DependencySeed.Filter() {
            @Override
            public boolean isAddOnFor(String type, String id) {
                return ArtifactType.TYPE_ECLIPSE_PRODUCT.equals(type) && productId.equals(id);
            }
        };
        for (FeatureRef feature : product.getFeatures()) {
            if (feature.getInstallMode() == FeatureRef.InstallMode.root) {
                // TODO 372780 get feature version from target platform that matches the specification; picking any version will no longer work once the the director installs from the target platform instead of from the resolved dependencies
                seeds.add(new DependencySeed(ArtifactType.TYPE_ECLIPSE_FEATURE, feature.getId(), null, filter));
                rootFeatures.add(feature.getId());
            }
        }
        return rootFeatures;
    }

    private File getExpandedLauncherBinaries() throws MojoExecutionException, MojoFailureException {
        // TODO 364134 take the executable feature from the target platform instead
        DependencyArtifacts dependencyArtifacts = TychoProjectUtils.getDependencyArtifacts(getProject());
        ArtifactDescriptor artifact = dependencyArtifacts.getArtifact(ArtifactType.TYPE_ECLIPSE_FEATURE,
                "org.eclipse.equinox.executable", null);

        if (artifact == null) {
            throw new MojoExecutionException("Unable to locate the equinox launcher feature (aka delta-pack)");
        }

        File equinoxExecFeature = artifact.getLocation();
        if (equinoxExecFeature.isDirectory()) {
            return equinoxExecFeature.getAbsoluteFile();
        } else {
            File unzipped = new File(getProject().getBuild().getDirectory(), artifact.getKey().getId() + "-"
                    + artifact.getKey().getVersion());
            if (unzipped.exists()) {
                return unzipped.getAbsoluteFile();
            }
            try {
                FileLocker locker = fileLockService.getFileLocker(equinoxExecFeature);
                locker.lock();
                try {
                    // unzip now then:
                    unzipped.mkdirs();
                    deflater.setSourceFile(equinoxExecFeature);
                    deflater.setDestDirectory(unzipped);
                    deflater.extract();
                    return unzipped.getAbsoluteFile();
                } finally {
                    locker.release();
                }
            } catch (ArchiverException e) {
                throw new MojoFailureException("Unable to unzip the eqiuinox executable feature", e);
            }
        }
    }

}
