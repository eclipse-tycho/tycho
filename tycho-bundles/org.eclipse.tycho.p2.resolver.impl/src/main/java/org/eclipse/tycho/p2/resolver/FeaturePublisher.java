/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
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
package org.eclipse.tycho.p2.resolver;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.target.repository.FileArtifactRepository;

@SuppressWarnings("restriction")
public class FeaturePublisher {

    public static void publishFeatures(List<Feature> features,
            BiConsumer<IArtifactDescriptor, IInstallableUnit> consumer, MavenLogger logger) {
        if (features.isEmpty()) {
            return;
        }
        PublisherInfo publisherInfo = new PublisherInfo();
        publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX);
        Map<IInstallableUnit, Feature> featureMap = new HashMap<>();
        FeaturesAction action = new FeaturesAction(features.toArray(Feature[]::new)) {
            @Override
            protected void publishFeatureArtifacts(Feature feature, IInstallableUnit featureIU,
                    IPublisherInfo publisherInfo) {
                //so not call super as we don't wan't to copy anything --> Bug in P2 with IPublisherInfo.A_INDEX option
            }

            @Override
            protected IInstallableUnit generateFeatureJarIU(Feature feature, IPublisherInfo publisherInfo) {
                IInstallableUnit iu = super.generateFeatureJarIU(feature, publisherInfo);
                featureMap.put(iu, feature);
                return iu;
            }
        };
        PublisherResult results = new PublisherResult();
        action.perform(publisherInfo, results, null);
        IQueryResult<IInstallableUnit> result = results.query(QueryUtil.ALL_UNITS, null);
        for (IInstallableUnit unit : result) {
            logger.debug("Publishing installable unit " + new VersionedId(unit.getId(), unit.getVersion()));
            Collection<IArtifactKey> artifacts = unit.getArtifacts();
            if (artifacts.isEmpty()) {
                consumer.accept(new IUArtifactDescriptor(unit), unit);
            } else {

                Feature publishedFeature = featureMap.get(unit);
                if (publishedFeature == null) {
                    //any other item
                    for (IArtifactKey key : artifacts) {
                        consumer.accept(new ArtifactDescriptor(key), unit);
                    }
                } else {
                    //The actual published feature...
                    for (IArtifactKey key : artifacts) {
                        IArtifactDescriptor fileDescriptor = FileArtifactRepository
                                .forFile(new File(publishedFeature.getLocation()), key);
                        consumer.accept(fileDescriptor, unit);
                    }
                }
            }

        }
    }

    public static boolean isMetadataOnly(IArtifactDescriptor descriptor) {
        return descriptor instanceof IUArtifactDescriptor;
    }

    private static final class IUArtifactDescriptor implements IArtifactDescriptor {

        private IInstallableUnit iu;
        private ArtifactKey artifactKey;

        public IUArtifactDescriptor(IInstallableUnit iu) {
            this.iu = iu;
        }

        @Override
        public IArtifactKey getArtifactKey() {
            if (artifactKey == null) {
                artifactKey = new ArtifactKey(PublisherHelper.IU_NAMESPACE, iu.getId(), iu.getVersion());
            }
            return artifactKey;
        }

        @Override
        public String getProperty(String key) {
            return null;
        }

        @Override
        public Map<String, String> getProperties() {
            return Collections.emptyMap();
        }

        @Override
        public IProcessingStepDescriptor[] getProcessingSteps() {
            return new IProcessingStepDescriptor[0];
        }

        @Override
        public IArtifactRepository getRepository() {
            return null;
        }

    }
}
