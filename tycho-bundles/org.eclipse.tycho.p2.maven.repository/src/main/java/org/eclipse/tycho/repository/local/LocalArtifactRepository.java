/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - #670 - Issue  Significant target-resolution runtime regression 
 *******************************************************************************/
package org.eclipse.tycho.repository.local;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.p2.maven.repository.Activator;
import org.eclipse.tycho.p2.maven.repository.xmlio.ArtifactsIO;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.LocalRepositoryReader;
import org.eclipse.tycho.p2.repository.RepositoryReader;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicies;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicy;
import org.eclipse.tycho.repository.p2base.artifact.repository.ArtifactRepositoryBaseImpl;

public class LocalArtifactRepository extends ArtifactRepositoryBaseImpl<GAVArtifactDescriptor> {

    private Set<IArtifactKey> descriptorsOnLastSave;
    private final LocalRepositoryP2Indices localRepoIndices;
    private final RepositoryReader contentLocator;
    private final Map<IArtifactKey, Lock> downloadLocks = new ConcurrentHashMap<>();

    // TODO what is the agent needed for? does using the default agent harm?
    public LocalArtifactRepository(LocalRepositoryP2Indices localRepoIndices) {
        this(Activator.getProvisioningAgent(), localRepoIndices);
    }

    public LocalArtifactRepository(LocalRepositoryP2Indices localRepoIndices, RepositoryReader contentLocator) {
        this(Activator.getProvisioningAgent(), localRepoIndices, contentLocator);
    }

    public LocalArtifactRepository(IProvisioningAgent agent, LocalRepositoryP2Indices localRepoIndices) {
        this(agent, localRepoIndices, new LocalRepositoryReader(localRepoIndices.getMavenContext()));
    }

    public LocalArtifactRepository(IProvisioningAgent agent, LocalRepositoryP2Indices localRepoIndices,
            RepositoryReader contentLocator) {
        super(agent, localRepoIndices.getBasedir().toURI(), ArtifactTransferPolicies.forLocalArtifacts());
        this.localRepoIndices = localRepoIndices;
        this.contentLocator = contentLocator;
        loadMaven();
    }

    private void loadMaven() {
        final ArtifactsIO io = new ArtifactsIO();
        TychoRepositoryIndex index = localRepoIndices.getArtifactsIndex();

        for (final GAV gav : index.getProjectGAVs()) {
            try {
                File localArtifactFileLocation = contentLocator.getLocalArtifactLocation(gav,
                        TychoConstants.CLASSIFIER_P2_ARTIFACTS, ArtifactType.TYPE_P2_ARTIFACTS);
                if (localArtifactFileLocation.isFile()) {
                    try (InputStream is = new FileInputStream(localArtifactFileLocation)) {
                        final Set<IArtifactDescriptor> gavDescriptors = io.readXML(is);
                        for (IArtifactDescriptor descriptor : gavDescriptors) {
                            if (ArtifactTransferPolicy.isCanonicalFormat(descriptor)
                                    && gav.getGroupId().startsWith(TychoConstants.P2_GROUPID_PREFIX)) {
                                //we must use the key to get the correct artifact GAV location
                                GAVArtifactDescriptor copy = new GAVArtifactDescriptor(descriptor.getArtifactKey());
                                //but retain the properties of the given descriptor
                                descriptor.getProperties().forEach(copy::setProperty);
                                copy.setProcessingSteps(descriptor.getProcessingSteps());
                                copy.setRepository(this);
                                internalAddDescriptor(copy);
                            } else {
                                internalAddDescriptor(descriptor);
                            }
                        }
                    }
                } else {
                    // if files have been manually removed from the repository, simply remove them from the index (bug 351080)
                    index.removeGav(gav);
                }
            } catch (IOException e) {
                index.removeGav(gav);
                localRepoIndices.getMavenContext().getLogger().debug("can't read stored meta-data", e);
            }
        }

        descriptorsOnLastSave = currentKeys();
    }

    public synchronized void save() {
        TychoRepositoryIndex index = localRepoIndices.getArtifactsIndex();

        ArtifactsIO io = new ArtifactsIO();

        Set<IArtifactKey> descriptors = currentKeys();

        for (IArtifactKey key : descriptors) {
            if (descriptorsOnLastSave.contains(key)) {
                continue;
            }
            Set<GAVArtifactDescriptor> keyDescriptors = descriptorsMap.get(key);
            if (keyDescriptors != null && !keyDescriptors.isEmpty()) {
                // all descriptors should have the same GAV
                GAVArtifactDescriptor anyDescriptorOfKey = keyDescriptors.iterator().next();
                GAV gav = anyDescriptorOfKey.getMavenCoordinates().getGav();
                index.addGav(gav);

                File file = contentLocator.getLocalArtifactLocation(gav, TychoConstants.CLASSIFIER_P2_ARTIFACTS,
                        ArtifactType.TYPE_P2_ARTIFACTS);
                file.getParentFile().mkdirs();
                localRepoIndices.getMavenContext().getLogger()
                        .debug("Writing P2 metadata for " + key + " to " + file + "...");
                try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
                    io.writeXML(keyDescriptors, os);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        try {
            index.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        descriptorsOnLastSave = new HashSet<>(descriptors);
    }

    protected HashSet<IArtifactKey> currentKeys() {
        return flattenedValues().map(IArtifactDescriptor::getArtifactKey)
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    protected File internalGetArtifactStorageLocation(IArtifactDescriptor descriptor) {
        String relativePath = toInternalDescriptor(descriptor).getMavenCoordinates()
                .getLocalRepositoryPath(localRepoIndices.getMavenContext());
        return new File(getBasedir(), relativePath);
    }

    @Override
    public GAVArtifactDescriptor createArtifactDescriptor(IArtifactKey key) {
        GAVArtifactDescriptor descriptor = new GAVArtifactDescriptor(key);
        descriptor.setRepository(this);
        return descriptor;
    }

    @Override
    protected IArtifactDescriptor getComparableDescriptor(IArtifactDescriptor descriptor) {
        // any descriptor can be converted to our internal type GAVArtifactDescriptor
        return toInternalDescriptor(descriptor);
    }

    @Override
    protected GAVArtifactDescriptor getInternalDescriptorForAdding(IArtifactDescriptor descriptor) {
        return toInternalDescriptor(descriptor);
    }

    private GAVArtifactDescriptor toInternalDescriptor(IArtifactDescriptor descriptor) {
        if (descriptor instanceof GAVArtifactDescriptor && descriptor.getRepository() == this) {
            return (GAVArtifactDescriptor) descriptor;
        } else {
            GAVArtifactDescriptor internalDescriptor = new GAVArtifactDescriptor(descriptor);
            internalDescriptor.setRepository(this);
            return internalDescriptor;
        }
    }

    private File getBasedir() {
        return new File(getLocation());
    }

    @Override
    public boolean isModifiable() {
        return true;
    }

    @Override
    public boolean isFileAlreadyAvailable(IArtifactKey artifactKey) {
        return contains(artifactKey);
    }

    Lock getLockForDownload(IArtifactKey key) {
        return downloadLocks.computeIfAbsent(key, k -> new ReentrantLock());
    }

    @Override
    protected void internalRemoveDescriptors(IArtifactKey key) {
        super.internalRemoveDescriptors(key);
        descriptorsOnLastSave.remove(key);
    }
}
