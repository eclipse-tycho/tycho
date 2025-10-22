/*******************************************************************************
 * Copyright (c) 2011, 2021 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - #225 MavenLogger is missing error method that accepts an exception
 *******************************************************************************/
package org.eclipse.tycho.core;

import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.query.ExpressionMatchQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.BuildDirectory;
import org.eclipse.tycho.p2.tools.FacadeException;

@Named
@Singleton
public class VerifierServiceImpl implements VerifierService {

    private final NullProgressMonitor monitor = new NullProgressMonitor();
    @Inject
    IProvisioningAgent agent;

    @Inject
    Logger logger;

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public boolean verify(URI metadataRepositoryUri, URI artifactRepositoryUri, BuildDirectory tempDirectory)
            throws FacadeException {
        logger.debug("Checking metadata from '" + metadataRepositoryUri + "' and artifacts from '"
                + artifactRepositoryUri + "'");
        try {
            final IMetadataRepository metadata = loadMetadataRepository(metadataRepositoryUri, agent);
            final IArtifactRepository artifactRepository = loadArtifactRepository(artifactRepositoryUri, agent);

            boolean valid = true;
            valid &= verifyReferencedArtifactsExist(metadata, artifactRepository, logger);
            valid &= verifyAllArtifactContent(artifactRepository, logger);
            if (valid) {
                logger.info("The integrity of the metadata repository '" + metadataRepositoryUri
                        + "' and artifact repository '" + artifactRepositoryUri + "' has been verified successfully");
            }
            return valid;
        } catch (ProvisionException e) {
            throw new FacadeException(e);
        }
    }

    private boolean verifyReferencedArtifactsExist(final IMetadataRepository metadata,
            final IArtifactRepository artifactRepository, Logger logger) {
        final IQueryResult<IInstallableUnit> collector = metadata.query(QueryUtil.ALL_UNITS, monitor);
        boolean valid = true;
        Set<IInstallableUnit> set = collector.toSet();
        logger.debug("Verifying content of " + set.size() + " units");
        for (IInstallableUnit iu : set) {
            final Collection<IArtifactKey> artifacts = iu.getArtifacts();
            for (IArtifactKey key : artifacts) {
                boolean verifyArtifactExists = verifyArtifactExists(key, artifactRepository, logger);
                logger.debug("Verify " + key + " exits: " + verifyArtifactExists);
                valid &= verifyArtifactExists;
            }
        }
        return valid;
    }

    private boolean verifyArtifactExists(IArtifactKey key, IArtifactRepository repository, Logger logger) {
        final IArtifactDescriptor[] descriptors = repository.getArtifactDescriptors(key);
        if (descriptors.length == 0) {
            logger.error("Missing artifact: " + key);
            return false;
        }
        return true;
    }

    private boolean verifyAllArtifactContent(IArtifactRepository repository, Logger logger) {
        boolean valid = true;

        IQueryResult<IArtifactKey> allKeys = repository
                .query(new ExpressionMatchQuery<>(IArtifactKey.class, ExpressionUtil.TRUE_EXPRESSION), null);
        Set<IArtifactKey> set = allKeys.toSet();
        logger.debug("Verifying content of " + set.size() + " artifacts");
        for (IArtifactKey key : set) {
            IArtifactDescriptor[] descriptors = repository.getArtifactDescriptors(key);
            for (IArtifactDescriptor descriptor : descriptors) {
                boolean verifyArtifactContent = verifyArtifactContent(repository, logger, descriptor);
                logger.debug("Verifying artifact content " + descriptor + ": " + verifyArtifactContent);
                valid &= verifyArtifactContent;
            }
        }
        return valid;
    }

    private boolean verifyArtifactContent(IArtifactRepository repository, Logger logger,
            IArtifactDescriptor descriptor) {
        final IStatus status = repository.getArtifact(descriptor, OutputStream.nullOutputStream(), monitor);
        if (!status.isOK()) {
            logStatus(status, "", logger::error);
        } else {
            logStatus(status, "", logger::debug);
        }
        return status.isOK();
    }

    private void logStatus(IStatus status, String indent, BiConsumer<String, Throwable> logger) {
        final Throwable exception = status.getException();
        if (exception == null) {
            logger.accept(indent + status.getMessage(), null);
        } else {
            logger.accept(indent + status.getMessage() + ": " + exception.getLocalizedMessage(), exception);
        }
        for (IStatus kid : status.getChildren()) {
            logStatus(kid, indent + "  ", logger);
        }
    }

    private IMetadataRepository loadMetadataRepository(URI metadataRepository, IProvisioningAgent agent)
            throws ProvisionException {
        final IMetadataRepositoryManager repositoryManager = agent.getService(IMetadataRepositoryManager.class);
        return repositoryManager.loadRepository(metadataRepository, monitor);
    }

    private IArtifactRepository loadArtifactRepository(URI artifactRepository, IProvisioningAgent agent)
            throws ProvisionException {
        final IArtifactRepositoryManager repositoryManager = agent.getService(IArtifactRepositoryManager.class);
        return repositoryManager.loadRepository(artifactRepository, monitor);
    }

}
