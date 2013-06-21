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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Properties;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.internal.p2.updatesite.CategoryXMLAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.actions.JREAction;
import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.target.ee.CustomEEResolutionHints;
import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherService;
import org.eclipse.tycho.repository.publishing.PublishingRepository;
import org.eclipse.tycho.repository.util.StatusTool;

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

    public Collection<?> publishEEProfile(File profileFile) throws FacadeException {
        validateProfile(profileFile);
        IPublisherAction jreAction = new JREAction(profileFile);
        Collection<IInstallableUnit> allIUs = executePublisher(jreAction, publishingRepository.getMetadataRepository(),
                publishingRepository.getArtifactRepository());
        return allIUs;
    }

    void validateProfile(File profileFile) throws FacadeException {
        Properties profileProperties = new Properties();
        try {
            FileInputStream stream = new FileInputStream(profileFile);
            try {
                profileProperties.load(stream);
                validateProfile(profileProperties, profileFile);
            } finally {
                stream.close();
            }
        } catch (IOException e) {
            throw new FacadeException(e);
        }
    }

    private void validateProfile(Properties props, File profileFile) throws FacadeException {
        String simpleFileName = profileFile.getName();
        if (!simpleFileName.endsWith(".profile")) {
            // otherwise JREAction will construct incorrect profile name
            throw new FacadeException("Profile file name must end with '.profile': " + profileFile);
        }

        String profileNameKey = "osgi.java.profile.name";
        String profileName = props.getProperty(profileNameKey);
        if (profileName == null) {
            throw new FacadeException("Mandatory property '" + profileNameKey + "' is missing in profile file "
                    + profileFile);
        }

        // make sure the profile name ends in a version
        new CustomEEResolutionHints(profileName);

        /*
         * To avoid surprises from bug 391805 in the JREAction (which will always use the profile
         * file name instead of the value specified as osgi.java.profile.name in the profile file),
         * require that these are the same.
         */
        String fileNamePrefix = simpleFileName.substring(0, simpleFileName.length() - ".profile".length()).toLowerCase(
                Locale.ENGLISH);
        if (!fileNamePrefix.equals(profileName.toLowerCase(Locale.ENGLISH))) {
            throw new FacadeException("Profile file with 'osgi.java.profile.name=" + profileName + "' must be named '"
                    + profileName + ".profile', but found file name: '" + simpleFileName + "'");
        }
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
