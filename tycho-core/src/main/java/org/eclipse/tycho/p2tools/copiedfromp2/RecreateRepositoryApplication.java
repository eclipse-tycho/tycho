/*******************************************************************************
 * Copyright (c) 2009, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype Inc - ongoing development
 *     Mykola Nikishov - multiple artifact checksums
 *******************************************************************************/

package org.eclipse.tycho.p2tools.copiedfromp2;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.artifact.processors.checksum.ChecksumUtilities;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.Messages;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.ArtifactKeyQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.osgi.util.NLS;

public class RecreateRepositoryApplication extends AbstractApplication {
    private URI repoLocation;
    private String repoName = null;
    boolean removeArtifactRepo = true;
    private Map<String, String> repoProperties = null;
    private Map<IArtifactKey, IArtifactDescriptor[]> repoMap = null;

    public RecreateRepositoryApplication(IProvisioningAgent agent) {
        super(agent);
    }

    @Override
    public IStatus run(IProgressMonitor monitor) throws ProvisionException {
        try {
            IArtifactRepository repository = initialize(monitor);
            removeRepository(repository, monitor);
            MultiStatus status = recreateRepository(monitor);
            if (status.isOK()) {
                return status;
            }
        } finally {
            if (removeArtifactRepo) {
                IArtifactRepositoryManager repositoryManager = getArtifactRepositoryManager();
                repositoryManager.removeRepository(repoLocation);
            }
        }

        return Status.OK_STATUS;
    }

    public void setArtifactRepository(URI repository) {
        this.repoLocation = repository;
    }

    private IArtifactRepository initialize(IProgressMonitor monitor) throws ProvisionException {
        IArtifactRepositoryManager repositoryManager = getArtifactRepositoryManager();
        removeArtifactRepo = !repositoryManager.contains(repoLocation);

        IArtifactRepository repository = repositoryManager.loadRepository(repoLocation,
                IRepositoryManager.REPOSITORY_HINT_MODIFIABLE, monitor);

        if (repository == null || !repository.isModifiable())
            throw new ProvisionException(
                    NLS.bind(Messages.exception_destinationNotModifiable, repository.getLocation()));
        if (!(repository instanceof IFileArtifactRepository))
            throw new ProvisionException(NLS.bind(Messages.exception_notLocalFileRepo, repository.getLocation()));

        repoName = repository.getName();
        repoProperties = repository.getProperties();

        repoMap = new HashMap<>();
        IQueryResult<IArtifactKey> keys = repository.query(ArtifactKeyQuery.ALL_KEYS, null);
        for (IArtifactKey key : keys) {
            IArtifactDescriptor[] descriptors = repository.getArtifactDescriptors(key);
            repoMap.put(key, descriptors);
        }

        return repository;
    }

    private void removeRepository(IArtifactRepository repository, IProgressMonitor monitor) throws ProvisionException {
        IArtifactRepositoryManager manager = getArtifactRepositoryManager();
        manager.removeRepository(repository.getLocation());

        boolean compressed = Boolean.parseBoolean(repoProperties.get(IRepository.PROP_COMPRESSED));
        URI realLocation = SimpleArtifactRepository.getActualLocation(repository.getLocation(), compressed);
        File realFile = URIUtil.toFile(realLocation);
        if (!realFile.exists() || !realFile.delete())
            throw new ProvisionException(NLS.bind(Messages.exception_unableToRemoveRepo, realFile.toString()));
    }

    private MultiStatus recreateRepository(IProgressMonitor monitor) throws ProvisionException {
        IArtifactRepositoryManager manager = getArtifactRepositoryManager();

        IArtifactRepository repository = manager.createRepository(repoLocation, repoName,
                IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, repoProperties);
        if (!(repository instanceof IFileArtifactRepository))
            throw new ProvisionException(NLS.bind(Messages.exception_notLocalFileRepo, repository.getLocation()));

        IFileArtifactRepository simple = (IFileArtifactRepository) repository;
        MultiStatus multiStatus = new MultiStatus(getClass(), 0, "Problem while recreate repository");
        repository.executeBatch(m -> {
            for (IArtifactKey key : repoMap.keySet()) {
                IArtifactDescriptor[] descriptors = repoMap.get(key);

                Set<File> files = new HashSet<>();
                for (IArtifactDescriptor descriptor : descriptors) {
                    File artifactFile = simple.getArtifactFile(descriptor);
                    files.add(artifactFile);

                    String size = Long.toString(artifactFile.length());

                    ArtifactDescriptor newDescriptor = new ArtifactDescriptor(descriptor);
                    newDescriptor.setProperty(IArtifactDescriptor.ARTIFACT_SIZE, size);
                    newDescriptor.setProperty(IArtifactDescriptor.DOWNLOAD_SIZE, size);

                    Map<String, String> checksums = new HashMap<>();
                    List<String> checksumsToSkip = Collections.emptyList();
                    IStatus status = ChecksumUtilities.calculateChecksums(artifactFile, checksums, checksumsToSkip);
                    if (!status.isOK()) {
                        multiStatus.add(status);
                    }
                    Map<String, String> checksumsToProperties = ChecksumUtilities
                            .checksumsToProperties(IArtifactDescriptor.DOWNLOAD_CHECKSUM, checksums);
                    //remove checksums that are no longer marked for publishing
                    String checksumProperty = IArtifactDescriptor.DOWNLOAD_CHECKSUM + ".";
                    for (String property : newDescriptor.getProperties().keySet().toArray(String[]::new)) {
                        if (property.startsWith(checksumProperty)) {
                            String id = property.substring(checksumProperty.length());
                            if (checksumsToProperties.containsKey(id)) {
                                continue;
                            }
                            newDescriptor.setProperty(checksumProperty + id, null);
                        }
                    }
                    //remove legacy property if present
                    if (!checksumsToProperties.containsKey("md5")) {
                        newDescriptor.setProperty("download.md5", null);
                    }
                    newDescriptor.addProperties(checksumsToProperties);
                    repository.addDescriptor(newDescriptor, null);
                }
            }
        }, monitor);
        return multiStatus;
    }
}
