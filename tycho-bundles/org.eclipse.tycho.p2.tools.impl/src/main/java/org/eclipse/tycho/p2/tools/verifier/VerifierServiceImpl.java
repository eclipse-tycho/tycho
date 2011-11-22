/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
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
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.tools.BuildOutputDirectory;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.impl.Activator;
import org.eclipse.tycho.p2.tools.verifier.facade.VerifierService;

public class VerifierServiceImpl implements VerifierService {

    private final NullProgressMonitor monitor = new NullProgressMonitor();
    private MavenContext mavenContext;

    public boolean verify(URI metadataRepositoryUri, URI artifactRepositoryUri, BuildOutputDirectory tempDirectory)
            throws FacadeException {
        MavenLogger logger = mavenContext.getLogger();
        logger.debug("Verifying metadata from " + metadataRepositoryUri + " with artifcats from "
                + artifactRepositoryUri);
        IProvisioningAgent agent = Activator.createProvisioningAgent(tempDirectory);
        try {
            try {
                final IMetadataRepository metadata = loadMetadataRepository(metadataRepositoryUri, agent);
                final IArtifactRepository artifactRepository = loadArtifactRepository(artifactRepositoryUri, agent);
                final IQueryResult<IInstallableUnit> collector = metadata.query(QueryUtil.ALL_UNITS, monitor);
                boolean valid = true;
                for (Iterator<IInstallableUnit> iterator = collector.iterator(); iterator.hasNext();) {
                    IInstallableUnit iu = iterator.next();
                    final Collection<IArtifactKey> artifacts = iu.getArtifacts();
                    for (IArtifactKey key : artifacts) {
                        valid &= verifySingleArtifact(key, artifactRepository, logger);
                    }
                }
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

    private boolean verifySingleArtifact(IArtifactKey key, IArtifactRepository repository, MavenLogger logger) {
        boolean valid = true;
        final IArtifactDescriptor[] descriptors = repository.getArtifactDescriptors(key);
        for (IArtifactDescriptor descriptor : descriptors) {
            final IStatus status = repository.getArtifact(descriptor, new ByteArrayOutputStream(), monitor);
            if (!status.isOK()) {
                logErrorStatus(status, "", logger);
            }
            valid &= status.isOK();
        }
        return valid;
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
        final IMetadataRepositoryManager repositoryManager = (IMetadataRepositoryManager) agent
                .getService(IMetadataRepositoryManager.SERVICE_NAME);
        return repositoryManager.loadRepository(metadataRepository, monitor);
    }

    private IArtifactRepository loadArtifactRepository(URI artifactRepository, IProvisioningAgent agent)
            throws ProvisionException {
        final IArtifactRepositoryManager repositoryManager = (IArtifactRepositoryManager) agent
                .getService(IArtifactRepositoryManager.SERVICE_NAME);
        return repositoryManager.loadRepository(artifactRepository, monitor);
    }

    public void setMavenContext(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
    }

}
