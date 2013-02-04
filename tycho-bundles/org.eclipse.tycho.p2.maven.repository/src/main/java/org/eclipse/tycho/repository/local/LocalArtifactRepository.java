/*******************************************************************************
 * Copyright (c) 2008, 2013 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
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
import java.util.Set;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.p2.maven.repository.Activator;
import org.eclipse.tycho.p2.maven.repository.xmlio.ArtifactsIO;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.LocalRepositoryReader;
import org.eclipse.tycho.p2.repository.MavenArtifactCoordinates;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.repository.RepositoryReader;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;
import org.eclipse.tycho.repository.p2base.artifact.provider.LocalArtifactTransferPolicy;
import org.eclipse.tycho.repository.p2base.artifact.repository.ArtifactRepositoryBaseImpl;

public class LocalArtifactRepository extends ArtifactRepositoryBaseImpl<GAVArtifactDescriptor> {

    private Set<IArtifactDescriptor> descriptorsOnLastSave;
    private final LocalRepositoryP2Indices localRepoIndices;
    private final RepositoryReader contentLocator;

    // TODO what is the agent needed for? does using the default agent harm?
    public LocalArtifactRepository(LocalRepositoryP2Indices localRepoIndices) {
        this(Activator.getProvisioningAgent(), localRepoIndices);
    }

    public LocalArtifactRepository(LocalRepositoryP2Indices localRepoIndices, RepositoryReader contentLocator) {
        this(Activator.getProvisioningAgent(), localRepoIndices, contentLocator);
    }

    public LocalArtifactRepository(IProvisioningAgent agent, LocalRepositoryP2Indices localRepoIndices) {
        this(agent, localRepoIndices, new LocalRepositoryReader(localRepoIndices.getBasedir()));
    }

    public LocalArtifactRepository(IProvisioningAgent agent, LocalRepositoryP2Indices localRepoIndices,
            RepositoryReader contentLocator) {
        super(agent, localRepoIndices.getBasedir().toURI(), new LocalArtifactTransferPolicy());
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
                        RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS, RepositoryLayoutHelper.EXTENSION_P2_ARTIFACTS);
                if (!localArtifactFileLocation.exists()) {
                    // if files have been manually removed from the repository, simply remove them from the index (bug 351080)
                    index.removeGav(gav);
                } else {
                    final InputStream is = new FileInputStream(contentLocator.getLocalArtifactLocation(gav,
                            RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS,
                            RepositoryLayoutHelper.EXTENSION_P2_ARTIFACTS));
                    try {
                        final Set<IArtifactDescriptor> gavDescriptors = io.readXML(is);
                        for (IArtifactDescriptor descriptor : gavDescriptors) {
                            internalAddDescriptor(descriptor);
                        }
                    } finally {
                        is.close();
                    }
                }
            } catch (IOException e) {
                // TODO throw properly typed exception if repository cannot be loaded
                e.printStackTrace();
            }
        }

        descriptorsOnLastSave = new HashSet<IArtifactDescriptor>(descriptors);
    }

    private void saveMaven() {
        File location = getBasedir();

        TychoRepositoryIndex index = localRepoIndices.getArtifactsIndex();

        ArtifactsIO io = new ArtifactsIO();

        Set<IArtifactDescriptor> changedDescriptors = new HashSet<IArtifactDescriptor>(descriptors);
        changedDescriptors.removeAll(descriptorsOnLastSave);

        Set<IArtifactKey> changedKeys = new HashSet<IArtifactKey>();
        for (IArtifactDescriptor changedDescriptor : changedDescriptors) {
            changedKeys.add(changedDescriptor.getArtifactKey());
        }

        for (IArtifactKey key : changedKeys) {
            Set<GAVArtifactDescriptor> keyDescriptors = descriptorsMap.get(key);
            if (keyDescriptors != null && !keyDescriptors.isEmpty()) {
                // all descriptors should have the same GAV
                GAVArtifactDescriptor anyDescriptorOfKey = keyDescriptors.iterator().next();
                GAV gav = anyDescriptorOfKey.getMavenCoordinates().getGav();
                index.addGav(gav);

                String relpath = getMetadataRelpath(gav);

                File file = new File(location, relpath);
                file.getParentFile().mkdirs();

                try {
                    OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
                    try {
                        io.writeXML(keyDescriptors, os);
                    } finally {
                        os.close();
                    }
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

        descriptorsOnLastSave = new HashSet<IArtifactDescriptor>(descriptors);
    }

    private String getMetadataRelpath(GAV gav) {
        String relpath = RepositoryLayoutHelper.getRelativePath(gav, RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS,
                RepositoryLayoutHelper.EXTENSION_P2_ARTIFACTS);
        return relpath;
    }

    // TODO 393004 store index on every write operation
//    @Override
//    protected void internalStore(IProgressMonitor monitor) {
//        // ...
//    }

    public void save() {
        saveMaven();
    }

    @Override
    protected File internalGetArtifactStorageLocation(IArtifactDescriptor descriptor) {
        MavenArtifactCoordinates mavenCoordinates = getInternalDescriptorForAdding(descriptor).getMavenCoordinates();
        GAV gav = mavenCoordinates.getGav();

        File basedir = getBasedir();
        String classifier = mavenCoordinates.getClassifier();
        String extension = mavenCoordinates.getExtension();
        File file = new File(basedir, RepositoryLayoutHelper.getRelativePath(gav, classifier, extension));
        return file;
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

}
