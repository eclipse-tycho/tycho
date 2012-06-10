/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.publisher.repo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactDescriptor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.actions.IPropertyAdvice;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.p2.impl.publisher.MavenPropertiesAdvice;
import org.eclipse.tycho.p2.impl.publisher.P2Artifact;
import org.eclipse.tycho.p2.impl.publisher.rootfiles.FeatureRootAdvice;
import org.eclipse.tycho.p2.metadata.IP2Artifact;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;

@SuppressWarnings("restriction")
public class FeatureRootfileArtifactRepository extends TransientArtifactRepository {
    private static final String ROOTFILE_CLASSIFIER = "root";

    private static final String ROOTFILE_EXTENSION = "zip";

    private final File outputDirectory;

    private final PublisherInfo publisherInfo;

    private Map<String, IP2Artifact> publishedArtifacts = new HashMap<String, IP2Artifact>();

    public FeatureRootfileArtifactRepository(PublisherInfo publisherInfo, File outputDirectory) {
        this.publisherInfo = publisherInfo;
        this.outputDirectory = outputDirectory;
    }

    @Override
    public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
        IArtifactKey artifactKey = descriptor.getArtifactKey();
        if (artifactKey != null && PublisherHelper.BINARY_ARTIFACT_CLASSIFIER.equals(artifactKey.getClassifier())) {
            try {
                return createRootfileOutputStream(artifactKey);
            } catch (IOException e) {
                throw new ProvisionException(e.getMessage(), e);
            }
        }

        return super.getOutputStream(descriptor);
    }

    private OutputStream createRootfileOutputStream(IArtifactKey artifactKey) throws ProvisionException, IOException {
        File outputFile = new File(this.outputDirectory, artifactKey.getId() + "-" + artifactKey.getVersion() + "-"
                + ROOTFILE_CLASSIFIER + "." + ROOTFILE_EXTENSION);

        OutputStream target = null;
        try {
            SimpleArtifactDescriptor simpleArtifactDescriptor = (SimpleArtifactDescriptor) createArtifactDescriptor(artifactKey);

            Collection<IPropertyAdvice> advices = publisherInfo.getAdvice(null, false, simpleArtifactDescriptor
                    .getArtifactKey().getId(), simpleArtifactDescriptor.getArtifactKey().getVersion(),
                    IPropertyAdvice.class);

            boolean mavenPropAdviceExists = false;
            for (IPropertyAdvice entry : advices) {
                if (entry instanceof MavenPropertiesAdvice) {
                    mavenPropAdviceExists = true;
                    entry.getArtifactProperties(null, simpleArtifactDescriptor);
                }
            }

            if (!mavenPropAdviceExists) {
                throw new ProvisionException("MavenPropertiesAdvice does not exist for artifact: "
                        + simpleArtifactDescriptor);
            }

            String mavenArtifactClassifier = getRootFileArtifactClassifier(simpleArtifactDescriptor.getArtifactKey()
                    .getId());
            simpleArtifactDescriptor.setProperty(RepositoryLayoutHelper.PROP_CLASSIFIER, mavenArtifactClassifier);
            simpleArtifactDescriptor.setProperty(RepositoryLayoutHelper.PROP_EXTENSION, ROOTFILE_EXTENSION);

            target = new BufferedOutputStream(new FileOutputStream(outputFile));

            this.publishedArtifacts.put(mavenArtifactClassifier,
                    new P2Artifact(outputFile, Collections.<IInstallableUnit> emptySet(), simpleArtifactDescriptor));

            descriptors.add(simpleArtifactDescriptor);
        } catch (FileNotFoundException e) {
            throw new ProvisionException(e.getMessage(), e);
        }

        return target;
    }

    String getRootFileArtifactClassifier(String artifactId) {
        List<IPublisherAdvice> adviceList = this.publisherInfo.getAdvice();

        for (IPublisherAdvice publisherAdvice : adviceList) {
            if (publisherAdvice instanceof FeatureRootAdvice) {
                String[] configurations = ((FeatureRootAdvice) publisherAdvice).getConfigurations();

                for (String config : configurations) {
                    if (!"".equals(config) && artifactId.endsWith(config)) {
                        return ROOTFILE_CLASSIFIER + "." + config;
                    }
                }
            }
        }

        return ROOTFILE_CLASSIFIER;
    }

    public Map<String, IP2Artifact> getPublishedArtifacts() {
        return publishedArtifacts;
    }
}
