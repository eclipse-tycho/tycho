/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - don't share publisher info instances between publisher calls (bug 346532)
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher;

import java.net.URI;

import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.TargetEnvironment;
import org.eclipse.tycho.repository.publishing.PublishingRepository;
import org.eclipse.tycho.repository.registry.ReactorRepositoryManager;

@SuppressWarnings("restriction")
class PublisherInfoTemplate {

    private final RepositoryReferences contextRepos;
    private final BuildContext context;

    private ReactorRepositoryManager reactorRepoManager;

    /**
     * Creates a template for creating configured PublisherInfo instances.
     * 
     * @param reactorRepositoryManager
     */
    public PublisherInfoTemplate(RepositoryReferences contextRepos, BuildContext context,
            ReactorRepositoryManager reactorRepositoryManager) {
        this.contextRepos = contextRepos;
        this.context = context;
        this.reactorRepoManager = reactorRepositoryManager;
    }

    public IPublisherInfo newPublisherInfo(PublishingRepository publishingRepo) throws FacadeException {
        final PublisherInfo publisherInfo = new PublisherInfo();

        publisherInfo.setMetadataRepository(publishingRepo.getMetadataRepository());
        publisherInfo.setArtifactRepository(publishingRepo.getArtifactRepository());
        publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX | IPublisherInfo.A_PUBLISH);

        setContextMetadataRepos(publisherInfo);
        // no (known) publisher action needs context artifact repositories

        setTargetEnvironments(publisherInfo);
        return publisherInfo;

    }

    private void setContextMetadataRepos(final PublisherInfo publisherInfo) {
        if (contextRepos.getMetadataRepositories().size() > 0) {
            final CompositeMetadataRepository contextMetadataComposite = CompositeMetadataRepository
                    .createMemoryComposite(reactorRepoManager.getAgent());
            for (URI repositoryLocation : contextRepos.getMetadataRepositories()) {
                contextMetadataComposite.addChild(repositoryLocation);
            }
            publisherInfo.setContextMetadataRepository(contextMetadataComposite);
        }
    }

    /**
     * Configure the list of target environments in the {@link PublisherInfo}. This information is
     * for example needed by the ProductAction which generates different configuration IUs for each
     * environment.
     */
    private void setTargetEnvironments(PublisherInfo publisherInfo) {
        int writeIx = 0;
        String[] configSpecs = new String[context.getEnvironments().size()];
        for (TargetEnvironment environment : context.getEnvironments()) {
            configSpecs[writeIx++] = environment.toConfigSpec();
        }
        publisherInfo.setConfigurations(configSpecs);
    }

}
