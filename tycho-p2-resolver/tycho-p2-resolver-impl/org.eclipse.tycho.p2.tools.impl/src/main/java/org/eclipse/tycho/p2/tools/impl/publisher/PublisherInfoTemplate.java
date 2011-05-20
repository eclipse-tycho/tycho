/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - don't share publisher info instances between publisher calls (bug 346532)
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.impl.publisher;

import java.io.File;
import java.net.URI;

import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.TargetEnvironment;

@SuppressWarnings("restriction")
class PublisherInfoTemplate {

    // not needed because the created repository is only an intermediate result
    private static final boolean TARGET_REPOSITORY_COMPRESS = false;
    private static final String TARGET_REPOSITORY_NAME = "publisher repository";

    private final File targetRepository;
    private final RepositoryReferences contextRepos;
    private final BuildContext context;

    private IProvisioningAgent agent;

    /**
     * Creates a template for creating configured PublisherInfo instances.
     * 
     * @param agent
     *            The provisioning agent for loading and creating p2 repositories. The constructed
     *            object takes the ownership for the passed agent instance.
     */
    public PublisherInfoTemplate(File targetRepository, RepositoryReferences contextRepos, BuildContext context,
            IProvisioningAgent agent) {
        this.targetRepository = targetRepository;
        this.contextRepos = contextRepos;
        this.context = context;
        this.agent = agent;
    }

    public PublisherInfo newPublisherInfo() throws FacadeException {
        checkRunning();

        try {
            final PublisherInfo publisherInfo = new PublisherInfo();

            setTargetMetadataRepository(publisherInfo);
            setTargetArtifactRepository(publisherInfo);

            setContextMetadataRepos(publisherInfo);
            // no (known) publisher action needs context artifact repositories

            setTargetEnvironments(publisherInfo);
            return publisherInfo;

        } catch (ProvisionException e) {
            throw new FacadeException(e);
        }
    }

    private void setTargetMetadataRepository(final PublisherInfo publisherInfo) throws ProvisionException {
        final boolean append = true;
        IMetadataRepository targetMetadataRepo = Publisher.createMetadataRepository(agent, targetRepository.toURI(),
                TARGET_REPOSITORY_NAME, append, TARGET_REPOSITORY_COMPRESS);
        publisherInfo.setMetadataRepository(targetMetadataRepo);
    }

    private void setTargetArtifactRepository(final PublisherInfo publisherInfo) throws ProvisionException {
        final boolean reusePackedFiles = false; // TODO check if we can/should use this
        final IArtifactRepository targetArtifactRepo = Publisher.createArtifactRepository(agent,
                targetRepository.toURI(), TARGET_REPOSITORY_NAME, TARGET_REPOSITORY_COMPRESS, reusePackedFiles);
        publisherInfo.setArtifactRepository(targetArtifactRepo);
        publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX | IPublisherInfo.A_PUBLISH);
    }

    private void setContextMetadataRepos(final PublisherInfo publisherInfo) {
        if (contextRepos.getMetadataRepositories().size() > 0) {
            final CompositeMetadataRepository contextMetadataComposite = CompositeMetadataRepository
                    .createMemoryComposite(agent);
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

    public void stopAgent() {
        if (agent != null) {
            agent.stop();
            agent = null;
        }
    }

    private void checkRunning() throws IllegalStateException {
        if (agent == null)
            throw new IllegalStateException("Attempt to access stopped instance"); //$NON-NLS-1$
    }
}
