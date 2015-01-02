/*******************************************************************************
 * Copyright (c) 2010, 2015 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - don't share publisher info instances between publisher calls (bug 346532)
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher;

import java.util.List;

import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.core.shared.TargetEnvironment;

@SuppressWarnings("restriction")
class PublisherInfoTemplate {

    private IMetadataRepository targetPlatformInstallableUnits;
    private List<TargetEnvironment> environments;

    /**
     * Creates a template for creating configured PublisherInfo instances.
     * 
     * @param reactorRepositoryManager
     */
    public PublisherInfoTemplate(IMetadataRepository targetPlatformInstallableUnits,
            List<TargetEnvironment> environments) {
        this.targetPlatformInstallableUnits = targetPlatformInstallableUnits;
        this.environments = environments;
    }

    public IPublisherInfo newPublisherInfo(IMetadataRepository metadataOutput, IArtifactRepository artifactsOutput) {
        final PublisherInfo publisherInfo = new PublisherInfo();

        publisherInfo.setMetadataRepository(metadataOutput);
        publisherInfo.setArtifactRepository(artifactsOutput);
        publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX | IPublisherInfo.A_PUBLISH);

        // TODO publishers only need an IQueryable<IInstallableUnit> -> changing this in p2 would simplify things for us
        publisherInfo.setContextMetadataRepository(targetPlatformInstallableUnits);
        // no (known) publisher action needs context artifact repositories

        setTargetEnvironments(publisherInfo);
        return publisherInfo;
    }

    /**
     * Configure the list of target environments in the {@link PublisherInfo}. This information is
     * for example needed by the ProductAction which generates different configuration IUs for each
     * environment.
     */
    private void setTargetEnvironments(PublisherInfo publisherInfo) {
        int writeIx = 0;
        String[] configSpecs = new String[environments.size()];
        for (TargetEnvironment environment : environments) {
            configSpecs[writeIx++] = environment.toConfigSpec();
        }
        publisherInfo.setConfigurations(configSpecs);
    }

}
