/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.internal.p2.updatesite.CategoryXMLAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherService;
import org.eclipse.tycho.p2.util.StatusTool;
import org.eclipse.tycho.repository.publishing.PublishingRepository;

@SuppressWarnings("restriction")
class PublisherServiceImpl implements PublisherService {

    private final BuildContext context;
    private final PublisherInfoTemplate configuration;
    private final PublishingRepository publishingRepository;
    private final MavenLogger logger;

    public PublisherServiceImpl(BuildContext context, PublisherInfoTemplate publisherConfiguration,
            PublishingRepository publishingRepository, MavenLogger logger) {
        this.context = context;
        this.configuration = publisherConfiguration;
        this.publishingRepository = publishingRepository;
        this.logger = logger;
    }

    public Collection<IInstallableUnit> publishCategories(File categoryDefinition) throws FacadeException,
            IllegalStateException {

        /*
         * At this point, we expect that the category.xml file does no longer contain any
         * "qualifier" literals; it is expected that they have been replaced before. Nevertheless we
         * pass the build qualifier to the CategoryXMLAction because this positively affects the IDs
         * of the category IUs (see {@link
         * org.eclipse.equinox.internal.p2.updatesite.SiteXMLAction#buildCategoryId(String)}).
         */
        CategoryXMLAction categoryXMLAction = new CategoryXMLAction(categoryDefinition.toURI(), context.getQualifier());

        /*
         * TODO Fix in Eclipse: category publisher should produce root IUs; workaround: the category
         * publisher produces no "inner" IUs, so just return all IUs
         */
        Collection<IInstallableUnit> allIUs = executePublisher(categoryXMLAction,
                publishingRepository.getMetadataRepository(), publishingRepository.getArtifactRepository());
        return allIUs;
    }

    public Collection<IInstallableUnit> publishProduct(File productDefinition, File launcherBinaries, String flavor)
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
        Collection<IInstallableUnit> allIUs = executePublisher(action, metadataRepository, artifactRepository);

        return selectUnit(allIUs, productDescriptor.getId());
    }

    private Collection<IInstallableUnit> executePublisher(IPublisherAction action,
            IMetadataRepository metadataRepository, IArtifactRepository artifactRepository) throws FacadeException {
        ResultSpyAction resultSpy = new ResultSpyAction();
        IPublisherAction[] actions = new IPublisherAction[] { action, resultSpy };
        Publisher publisher = new Publisher(configuration.newPublisherInfo(metadataRepository, artifactRepository));

        IStatus result = publisher.publish(actions, null);
        handlePublisherStatus(result);

        return resultSpy.getAllIUs();
    }

    private void handlePublisherStatus(IStatus result) throws FacadeException {
        if (result.matches(IStatus.INFO)) {
            logger.info(StatusTool.collectProblems(result));
        } else if (result.matches(IStatus.WARNING)) {
            logger.warn(StatusTool.collectProblems(result));
        } else if (!result.isOK()) {
            throw new FacadeException(StatusTool.collectProblems(result), result.getException());
        }
    }

    private Collection<IInstallableUnit> selectUnit(Collection<IInstallableUnit> units, String id) {
        for (IInstallableUnit unit : units) {
            if (id.equals(unit.getId())) {
                return Collections.singleton(unit);
            }
        }
        throw new AssertionFailedException("Publisher did not produce expected IU");
    }
}
