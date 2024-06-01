/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.publisher;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.actions.IPropertyAdvice;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.p2.metadata.IP2Artifact;
import org.eclipse.tycho.p2.publisher.P2Artifact;
import org.eclipse.tycho.p2.publisher.TransientArtifactRepository;
import org.eclipse.tycho.p2.publisher.rootfiles.FeatureRootAdvice;
import org.eclipse.tycho.p2maven.advices.MavenPropertiesAdvice;

public class FeatureRootfileArtifactRepository extends TransientArtifactRepository {
    //for backward compatibility
    @SuppressWarnings("deprecation")
    private static final String PROP_EXTENSION = TychoConstants.PROP_EXTENSION;

    private final File outputDirectory;

    private final PublisherInfo publisherInfo;

    private Map<File, IArtifactKey> artifactsToPublish = new HashMap<>();

    private Map<String, IP2Artifact> collect;

    private List<IArtifactDescriptor> temp = new ArrayList<>();

    public FeatureRootfileArtifactRepository(PublisherInfo publisherInfo, File outputDirectory) {
        this.publisherInfo = publisherInfo;
        this.outputDirectory = outputDirectory;
    }

    @Override
    public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
        IArtifactKey artifactKey = descriptor.getArtifactKey();
        if (artifactKey != null && PublisherHelper.BINARY_ARTIFACT_CLASSIFIER.equals(artifactKey.getClassifier())) {
            if (!publisherInfo
                    .getAdvice(null, false, artifactKey.getId(), artifactKey.getVersion(), IPropertyAdvice.class)
                    .stream().anyMatch(advice -> advice instanceof MavenPropertiesAdvice)) {
                throw new ProvisionException("MavenPropertiesAdvice does not exist for artifact: " + artifactKey);
            }
            File outputFile = new File(this.outputDirectory, artifactKey.getId() + "-" + artifactKey.getVersion() + "-"
                    + TychoConstants.ROOTFILE_CLASSIFIER + "." + TychoConstants.ROOTFILE_EXTENSION);
            try {
                temp.add(descriptor);
                descriptors.add(descriptor);
                artifactsToPublish.put(outputFile, artifactKey);
                return new BufferedOutputStream(new FileOutputStream(outputFile));
            } catch (IOException e) {
                throw new ProvisionException(e.getMessage(), e);
            }
        }

        return super.getOutputStream(descriptor);
    }

    String getRootFileArtifactClassifier(String artifactId) {
        List<IPublisherAdvice> adviceList = this.publisherInfo.getAdvice();

        for (IPublisherAdvice publisherAdvice : adviceList) {
            if (publisherAdvice instanceof FeatureRootAdvice featureRootAdvice) {
                String[] configurations = featureRootAdvice.getConfigurations();

                for (String config : configurations) {
                    if (!"".equals(config) && artifactId.endsWith(config)) {
                        return TychoConstants.ROOTFILE_CLASSIFIER + "." + config;
                    }
                }
            }
        }

        return TychoConstants.ROOTFILE_CLASSIFIER;
    }

    public Map<String, IP2Artifact> getPublishedArtifacts() {
        if (collect == null) {
            this.descriptors.removeAll(temp);
            collect = artifactsToPublish.entrySet().stream().map(entry -> {
                File outputFile = entry.getKey();
                IArtifactKey artifactKey = entry.getValue();
                IArtifactDescriptor artifactDescriptor = PublisherHelper.createArtifactDescriptor(publisherInfo,
                        artifactKey, outputFile);
                Collection<IPropertyAdvice> advices = publisherInfo.getAdvice(null, false,
                        artifactDescriptor.getArtifactKey().getId(), artifactDescriptor.getArtifactKey().getVersion(),
                        IPropertyAdvice.class);

                for (IPropertyAdvice advice : advices) {
                    if (advice instanceof MavenPropertiesAdvice) {
                        advice.getArtifactProperties(null, artifactDescriptor);
                    }
                }
                String mavenArtifactClassifier = getRootFileArtifactClassifier(
                        artifactDescriptor.getArtifactKey().getId());
                if (artifactDescriptor instanceof ArtifactDescriptor impl) {
                    impl.setProperty(TychoConstants.PROP_CLASSIFIER, mavenArtifactClassifier);
                    //Type and extension are the same for rootfiles ...
                    impl.setProperty(PROP_EXTENSION, TychoConstants.ROOTFILE_EXTENSION);
                    impl.setProperty(TychoConstants.PROP_TYPE, TychoConstants.ROOTFILE_EXTENSION);
                }
                addDescriptor(artifactDescriptor);
                return Map.entry(mavenArtifactClassifier,
                        new P2Artifact(outputFile, Collections.<IInstallableUnit> emptySet(), artifactDescriptor));
            }).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        }
        return collect;
    }
}
