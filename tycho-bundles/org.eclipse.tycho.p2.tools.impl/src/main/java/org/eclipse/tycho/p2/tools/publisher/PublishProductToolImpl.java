/*******************************************************************************
 * Copyright (c) 2010, 2015 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher;

import static org.eclipse.tycho.p2.tools.publisher.DependencySeedUtil.createSeed;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.publisher.facade.PublishProductTool;
import org.eclipse.tycho.repository.publishing.PublishingRepository;

@SuppressWarnings("restriction")
public class PublishProductToolImpl implements PublishProductTool {

    private PublisherActionRunner publisherRunner;
    private PublishingRepository publishingRepository;

    public PublishProductToolImpl(PublisherActionRunner publisherRunner, PublishingRepository publishingRepository) {
        this.publisherRunner = publisherRunner;
        this.publishingRepository = publishingRepository;
    }

    @Override
    public Collection<DependencySeed> publishProduct(File productDefinition, File launcherBinaries, String flavor)
            throws FacadeException, IllegalStateException {

        IProductDescriptor productDescriptor = null;
        try {
            productDescriptor = new ProductFile(productDefinition.getAbsolutePath());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to load product file " + productDefinition.getAbsolutePath(), e); //$NON-NLS-1$
        }

        ProductAction action = new ProductAction(null, productDescriptor, flavor, launcherBinaries);
        IMetadataRepository metadataRepository = publishingRepository.getMetadataRepository();
        IArtifactRepository artifactRepository = publishingRepository
                .getArtifactRepositoryForWriting(new ProductBinariesWriteSession(productDescriptor.getId()));
        Collection<IInstallableUnit> allIUs = publisherRunner.executeAction(action, metadataRepository,
                artifactRepository);

        return Collections.singletonList(createSeed(ArtifactType.TYPE_ECLIPSE_PRODUCT,
                selectUnit(allIUs, productDescriptor.getId())));
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
