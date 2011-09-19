/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.impl.publisher;

import java.io.File;
import java.net.URI;
import java.util.List;

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
import org.eclipse.tycho.p2.tools.impl.Activator;
import org.eclipse.tycho.p2.tools.publisher.PublisherService;
import org.eclipse.tycho.p2.tools.publisher.PublisherServiceFactory;

@SuppressWarnings("restriction")
public class PublisherServiceFactoryImpl implements PublisherServiceFactory {
    // not needed because the created repository is only an intermediate result
    private static final boolean TARGET_REPOSITORY_COMPRESS = false;

    private static final String TARGET_REPOSITORY_NAME = "publisher repository";

    public PublisherService createPublisher(File targetRepository, RepositoryReferences contextRepos,
            BuildContext context) throws FacadeException {
        // create an own instance of the provisioning agent to prevent cross talk with other things
        // that happen in the Tycho OSGi runtime
        IProvisioningAgent agent = Activator.createProvisioningAgent(context.getTargetDirectory());

        try {
            final PublisherInfo publisherInfo = new PublisherInfo();

            setTargetMetadataRepository(publisherInfo, targetRepository, agent);
            setTargetArtifactRepository(publisherInfo, targetRepository, agent);

            setContextMetadataRepos(publisherInfo, contextRepos, agent);
            // no (known) publisher action needs context artifact repositories

            setTargetEnvironments(publisherInfo, context.getEnvironments());

            return new PublisherServiceImpl(context, publisherInfo, agent);
        } catch (ProvisionException e) {
            agent.stop();
            throw new FacadeException(e);
        }
    }

    private static void setTargetMetadataRepository(final PublisherInfo publisherInfo, File location,
            IProvisioningAgent agent) throws ProvisionException {
        final boolean append = true;
        IMetadataRepository targetMetadataRepo = Publisher.createMetadataRepository(agent, location.toURI(),
                TARGET_REPOSITORY_NAME, append, TARGET_REPOSITORY_COMPRESS);
        publisherInfo.setMetadataRepository(targetMetadataRepo);
    }

    private static void setTargetArtifactRepository(final PublisherInfo publisherInfo, File location,
            IProvisioningAgent agent) throws ProvisionException {
        final boolean reusePackedFiles = false; // TODO check if we can/should use this
        final IArtifactRepository targetArtifactRepo = Publisher.createArtifactRepository(agent, location.toURI(),
                TARGET_REPOSITORY_NAME, TARGET_REPOSITORY_COMPRESS, reusePackedFiles);
        publisherInfo.setArtifactRepository(targetArtifactRepo);
        publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX | IPublisherInfo.A_PUBLISH);
    }

    private static void setContextMetadataRepos(final PublisherInfo publisherInfo, RepositoryReferences contextRepos,
            IProvisioningAgent agent) {
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
    private static void setTargetEnvironments(PublisherInfo publisherInfo, List<TargetEnvironment> environments) {
        int writeIx = 0;
        String[] configSpecs = new String[environments.size()];
        for (TargetEnvironment environment : environments) {
            configSpecs[writeIx++] = environment.toConfigSpec();
        }
        publisherInfo.setConfigurations(configSpecs);
    }
}
