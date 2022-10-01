/*******************************************************************************
 * Copyright (c) 2010, 2015 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Properties;

import org.eclipse.equinox.internal.p2.updatesite.CategoryXMLAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.actions.JREAction;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.p2.target.ee.CustomEEResolutionHints;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherService;
import org.eclipse.tycho.repository.publishing.PublishingRepository;

@SuppressWarnings("restriction")
public class PublisherServiceImpl implements PublisherService {

    private final PublisherActionRunner publisherRunner;
    private final String qualifier;
    private final PublishingRepository publishingRepository;

    public PublisherServiceImpl(PublisherActionRunner publisherRunner, String qualifier,
            PublishingRepository publishingRepository) {
        this.publisherRunner = publisherRunner;
        this.qualifier = qualifier;
        this.publishingRepository = publishingRepository;
    }

    @Override
    public Collection<DependencySeed> publishCategories(File categoryDefinition)
            throws FacadeException, IllegalStateException {

        /*
         * At this point, we expect that the category.xml file does no longer contain any
         * "qualifier" literals; it is expected that they have been replaced before. Nevertheless we
         * pass the build qualifier to the CategoryXMLAction because this positively affects the IDs
         * of the category IUs (see {@link
         * org.eclipse.equinox.internal.p2.updatesite.SiteXMLAction#buildCategoryId(String)}).
         */
        CategoryXMLAction categoryXMLAction = new CategoryXMLAction(categoryDefinition.toURI(), qualifier);

        /*
         * TODO Fix in Eclipse: category publisher should produce root IUs; workaround: the category
         * publisher produces no "inner" IUs, so just return all IUs
         */
        Collection<IInstallableUnit> allIUs = publisherRunner.executeAction(categoryXMLAction,
                publishingRepository.getMetadataRepository(), publishingRepository.getArtifactRepository());
        // TODO introduce type "eclipse-category"?
        return toSeeds(null, allIUs);
    }

    @Override
    public Collection<DependencySeed> publishEEProfile(File profileFile) throws FacadeException {
        validateProfile(profileFile);
        IPublisherAction jreAction = new JREAction(profileFile);
        Collection<IInstallableUnit> allIUs = publisherRunner.executeAction(jreAction,
                publishingRepository.getMetadataRepository(), publishingRepository.getArtifactRepository());
        return toSeeds(null, allIUs);
    }

    @Override
    public Collection<DependencySeed> publishEEProfile(String profileName) throws FacadeException {
        IPublisherAction jreAction = new JREAction(profileName);
        Collection<IInstallableUnit> allIUs = publisherRunner.executeAction(jreAction,
                publishingRepository.getMetadataRepository(), publishingRepository.getArtifactRepository());
        return toSeeds(null, allIUs);
    }

    @Override
    public Collection<DependencySeed> publishEEProfile(ExecutionEnvironment ee) throws FacadeException {
        IPublisherAction jreAction = new ExecutionEnvironmentAction(ee);
        Collection<IInstallableUnit> allIUs = publisherRunner.executeAction(jreAction,
                publishingRepository.getMetadataRepository(), publishingRepository.getArtifactRepository());
        return toSeeds(null, allIUs);
    }

    void validateProfile(File profileFile) throws FacadeException {
        Properties profileProperties = new Properties();
        try {
            try (FileInputStream stream = new FileInputStream(profileFile)) {
                profileProperties.load(stream);
                validateProfile(profileProperties, profileFile);
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
            throw new FacadeException(
                    "Mandatory property '" + profileNameKey + "' is missing in profile file " + profileFile);
        }

        // make sure the profile name ends in a version
        new CustomEEResolutionHints(profileName);

        /*
         * To avoid surprises from bug 391805 in the JREAction (which will always use the profile
         * file name instead of the value specified as osgi.java.profile.name in the profile file),
         * require that these are the same.
         */
        String fileNamePrefix = simpleFileName.substring(0, simpleFileName.length() - ".profile".length())
                .toLowerCase(Locale.ENGLISH);
        if (!fileNamePrefix.equals(profileName.toLowerCase(Locale.ENGLISH))) {
            throw new FacadeException("Profile file with 'osgi.java.profile.name=" + profileName + "' must be named '"
                    + profileName + ".profile', but found file name: '" + simpleFileName + "'");
        }
    }

    private static Collection<DependencySeed> toSeeds(String type, Collection<IInstallableUnit> units) {
        Collection<DependencySeed> result = new ArrayList<>(units.size());
        for (IInstallableUnit unit : units) {
            result.add(DependencySeedUtil.createSeed(type, unit));
        }
        return result;
    }
}
