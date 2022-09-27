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

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
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

@Component(role = VerifierService.class)
public class VerifierServiceImpl implements VerifierService {

    private final NullProgressMonitor monitor = new NullProgressMonitor();
    @Requirement
    IProvisioningAgent agent;

    @Requirement
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
        for (Iterator<IInstallableUnit> iterator = collector.iterator(); iterator.hasNext();) {
            IInstallableUnit iu = iterator.next();
            final Collection<IArtifactKey> artifacts = iu.getArtifacts();
            for (IArtifactKey key : artifacts) {
                valid &= verifyArtifactExists(key, artifactRepository, logger);
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
        for (Iterator<IArtifactKey> keyIt = allKeys.iterator(); keyIt.hasNext();) {
            IArtifactKey key = keyIt.next();

            IArtifactDescriptor[] descriptors = repository.getArtifactDescriptors(key);
            for (IArtifactDescriptor descriptor : descriptors) {
                valid &= verifyArtifactContent(repository, logger, descriptor);
            }
        }
        return valid;
    }

    private boolean verifyArtifactContent(IArtifactRepository repository, Logger logger,
            IArtifactDescriptor descriptor) {
        final IStatus status = repository.getArtifact(descriptor, new ByteArrayOutputStream(), monitor);
        if (!status.isOK()) {
            logErrorStatus(status, "", logger);
        }
        return status.isOK();
    }

    private void logErrorStatus(IStatus status, String indent, Logger logger) {
        final Throwable exception = status.getException();
        if (exception == null) {
            logger.error(indent + status.getMessage());
        } else {
            logger.error(indent + status.getMessage() + ": " + exception.getLocalizedMessage(), exception);
        }
        for (IStatus kid : status.getChildren()) {
            logErrorStatus(kid, indent + "  ", logger);
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
