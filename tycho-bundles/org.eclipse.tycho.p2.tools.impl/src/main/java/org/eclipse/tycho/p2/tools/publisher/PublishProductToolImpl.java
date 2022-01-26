/*******************************************************************************
 * Copyright (c) 2010, 2021 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - [Issue #80] Incorrect requirement version for configuration/plugins in publish-products (gently sponsored by Compart AG)
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher;

import static org.eclipse.tycho.p2.tools.publisher.DependencySeedUtil.createSeed;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.AdviceFileAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.Interpolator;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.core.shared.BuildFailureException;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.target.ArtifactTypeHelper;
import org.eclipse.tycho.p2.target.P2TargetPlatform;
import org.eclipse.tycho.p2.tools.publisher.facade.PublishProductTool;
import org.eclipse.tycho.repository.publishing.PublishingRepository;

/**
 * Tool for transforming product definition source files into p2 metadata and artifacts. Includes
 * more steps than classic publisher would do, e.g. qualifier expansion.
 */
@SuppressWarnings("restriction")
public class PublishProductToolImpl implements PublishProductTool {

    private final P2TargetPlatform targetPlatform;

    private final PublisherActionRunner publisherRunner;
    private final PublishingRepository publishingRepository;

    private final String buildQualifier;
    private final Interpolator interpolator;
    private final MavenLogger logger;

    public PublishProductToolImpl(PublisherActionRunner publisherRunner, PublishingRepository publishingRepository,
            P2TargetPlatform targetPlatform, String buildQualifier, Interpolator interpolator, MavenLogger logger) {
        this.publisherRunner = publisherRunner;
        this.publishingRepository = publishingRepository;
        this.targetPlatform = targetPlatform;
        this.buildQualifier = buildQualifier;
        this.interpolator = interpolator;
        this.logger = logger;
    }

    @Override
    public List<DependencySeed> publishProduct(File productFile, File launcherBinaries, String flavor)
            throws IllegalArgumentException {

        IProductDescriptor originalProduct = loadProductFile(productFile);
        ExpandedProduct expandedProduct = new ExpandedProduct(originalProduct, buildQualifier, targetPlatform,
                interpolator, logger);

        IPublisherAdvice[] advice = getProductSpecificAdviceFileAdvice(productFile, expandedProduct);

        ProductAction action = new ProductAction(null, expandedProduct, flavor, launcherBinaries);
        IMetadataRepository metadataRepository = publishingRepository.getMetadataRepository();
        IArtifactRepository artifactRepository = publishingRepository
                .getArtifactRepositoryForWriting(new ProductBinariesWriteSession(expandedProduct.getId()));
        Collection<IInstallableUnit> allIUs = publisherRunner.executeAction(action, metadataRepository,
                artifactRepository, advice);

        List<DependencySeed> seeds = new ArrayList<>();
        seeds.add(createSeed(ArtifactType.TYPE_ECLIPSE_PRODUCT, selectUnit(allIUs, expandedProduct.getId())));
        addRootFeatures(expandedProduct, seeds);
        return seeds;
    }

    /**
     * In addition to the p2.inf file in the project root (which is automatically picked up by p2,
     * see see {@link ProductAction#createAdviceFileAdvice()}), we allow a "xx.p2.inf" next to a
     * product file "xx.product".
     */
    private static IPublisherAdvice[] getProductSpecificAdviceFileAdvice(File productFile,
            IProductDescriptor expandedProduct) {
        AdviceFileAdvice advice = new AdviceFileAdvice(expandedProduct.getId(),
                Version.parseVersion(expandedProduct.getVersion()), new Path(productFile.getParent()),
                new Path(getProductSpecificP2InfName(productFile.getName())));
        if (advice.containsAdvice()) {
            return new IPublisherAdvice[] { advice };
        } else {
            return new IPublisherAdvice[0];
        }
    }

    private static String getProductSpecificP2InfName(String productFileName) {
        // This must match org.eclipse.tycho.p2.impl.publisher.ProductDependenciesAction.addPublisherAdvice(IPublisherInfo)
        final String p2infFilename = productFileName.substring(0, productFileName.length() - ".product".length())
                + ".p2.inf";
        return p2infFilename;
    }

    private static void addRootFeatures(ExpandedProduct product, List<DependencySeed> seeds) {
        final String productId = product.getId();

        // add root features as special dependency seed which are marked as "add-on" for the product
        DependencySeed.Filter filter = (type, id) -> ArtifactType.TYPE_ECLIPSE_PRODUCT.equals(type)
                && productId.equals(id);
        for (IInstallableUnit featureIU : product.getRootFeatures()) {
            ArtifactKey featureArtifact = ArtifactTypeHelper.toTychoArtifact(featureIU);
            seeds.add(new DependencySeed(featureArtifact.getType(), featureArtifact.getId(), featureIU, filter));
        }
    }

    private static IProductDescriptor loadProductFile(File productFile) throws IllegalArgumentException {
        try {
            return new ProductFile(productFile.getAbsolutePath());
        } catch (Exception e) {
            throw new BuildFailureException(
                    "Cannot parse product file " + productFile.getAbsolutePath() + ": " + e.getMessage(), e); //$NON-NLS-1$
        }
    }

    private static IInstallableUnit selectUnit(Collection<IInstallableUnit> units, String id) {
        for (IInstallableUnit unit : units) {
            if (id.equals(unit.getId())) {
                return unit;
            }
        }
        throw new AssertionFailedException("Publisher did not produce expected IU");
    }

}
