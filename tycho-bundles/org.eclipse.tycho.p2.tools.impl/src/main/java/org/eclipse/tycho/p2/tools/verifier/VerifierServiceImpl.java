/*******************************************************************************
 * Copyright (c) 2011, 2020 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.verifier;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;

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
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.impl.Activator;
import org.eclipse.tycho.p2.tools.verifier.facade.VerifierService;

public class VerifierServiceImpl implements VerifierService {

    private final NullProgressMonitor monitor = new NullProgressMonitor();
    private MavenContext mavenContext;

    @Override
    public boolean verify(URI metadataRepositoryUri, URI artifactRepositoryUri, BuildDirectory tempDirectory)
            throws FacadeException {
        MavenLogger logger = mavenContext.getLogger();
        logger.debug("Checking metadata from '" + metadataRepositoryUri + "' and artifacts from '"
                + artifactRepositoryUri + "'");
        IProvisioningAgent agent = Activator.createProvisioningAgent(tempDirectory);
        try {
            try {
                final IMetadataRepository metadata = loadMetadataRepository(metadataRepositoryUri, agent);
                final IArtifactRepository artifactRepository = loadArtifactRepository(artifactRepositoryUri, agent);

                boolean valid = true;
                valid &= verifyReferencedArtifactsExist(metadata, artifactRepository, logger);
                valid &= verifyAllArtifactContent(artifactRepository, logger);
                if (valid) {
                    logger.info("The integrity of the metadata repository '" + metadataRepositoryUri
                            + "' and artifact repository '" + artifactRepositoryUri
                            + "' has been verified successfully");
                }
                return valid;
            } catch (ProvisionException e) {
                throw new FacadeException(e);
            }
        } finally {
            agent.stop();
        }
    }

    private boolean verifyReferencedArtifactsExist(final IMetadataRepository metadata,
            final IArtifactRepository artifactRepository, MavenLogger logger) {
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

    private boolean verifyArtifactExists(IArtifactKey key, IArtifactRepository repository, MavenLogger logger) {
        final IArtifactDescriptor[] descriptors = repository.getArtifactDescriptors(key);
        if (descriptors.length == 0) {
            logger.error("Missing artifact: " + key);
            return false;
        }
        return true;
    }

    private boolean verifyAllArtifactContent(IArtifactRepository repository, MavenLogger logger) {
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

    private boolean verifyArtifactContent(IArtifactRepository repository, MavenLogger logger,
            IArtifactDescriptor descriptor) {
        final IStatus status = repository.getArtifact(descriptor, new ByteArrayOutputStream(), monitor);
        if (!status.isOK()) {
            logErrorStatus(status, "", logger);
        }
        return status.isOK();
    }

    private void logErrorStatus(IStatus status, String indent, MavenLogger logger) {
        final Throwable exception = status.getException();
        if (exception == null) {
            logger.error(indent + status.getMessage());
        } else {
            logger.error(indent + status.getMessage() + ": " + exception.getLocalizedMessage());
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

    public void setMavenContext(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
    }

}
