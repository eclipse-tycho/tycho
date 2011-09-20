/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.maven.repository;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.tycho.p2.maven.repository.xmlio.ArtifactsIO;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.LocalRepositoryReader;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.repository.RepositoryReader;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;

public class LocalArtifactRepository extends AbstractMavenArtifactRepository {

    private final Set<IArtifactKey> changedDescriptors = new HashSet<IArtifactKey>();
    private final LocalRepositoryP2Indices localRepoIndices;

    public LocalArtifactRepository(LocalRepositoryP2Indices localRepoIndices) {
        this(Activator.getProvisioningAgent(), localRepoIndices);
    }

    public LocalArtifactRepository(IProvisioningAgent agent, LocalRepositoryP2Indices localRepoIndices) {
        super(agent, localRepoIndices.getBasedir().toURI(), localRepoIndices.getArtifactsIndex(),
                new LocalRepositoryReader(localRepoIndices.getBasedir()));
        this.localRepoIndices = localRepoIndices;
    }

    public LocalArtifactRepository(LocalRepositoryP2Indices localRepoIndices, RepositoryReader contentLocator) {
        super(Activator.getProvisioningAgent(), localRepoIndices.getBasedir().toURI(), localRepoIndices
                .getArtifactsIndex(), contentLocator);
        this.localRepoIndices = localRepoIndices;
    }

    private void saveMaven() {
        File location = getBasedir();

        TychoRepositoryIndex index = localRepoIndices.getArtifactsIndex();

        ArtifactsIO io = new ArtifactsIO();

        for (IArtifactKey key : changedDescriptors) {
            Set<IArtifactDescriptor> keyDescriptors = descriptorsMap.get(key);
            if (keyDescriptors != null && !keyDescriptors.isEmpty()) {
                IArtifactDescriptor random = keyDescriptors.iterator().next();
                GAV gav = RepositoryLayoutHelper.getGAV(random.getProperties());

                if (gav == null) {
                    gav = getP2GAV(random);
                }

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

        changedDescriptors.clear();
    }

    private String getMetadataRelpath(GAV gav) {
        String relpath = RepositoryLayoutHelper.getRelativePath(gav, RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS,
                RepositoryLayoutHelper.EXTENSION_P2_ARTIFACTS);
        return relpath;
    }

    public void save() {
        saveMaven();
    }

    @Override
    public synchronized OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
        GAV gav = RepositoryLayoutHelper.getGAV(descriptor.getProperties());

        if (gav == null) {
            gav = getP2GAV(descriptor);
        }

        File basedir = getBasedir();
        File file = new File(basedir, RepositoryLayoutHelper.getRelativePath(gav, null, null));
        file.getParentFile().mkdirs();

        // TODO ideally, repository index should be updated after artifact has been written to the file

        ArtifactDescriptor newDescriptor = new ArtifactDescriptor(descriptor);
        newDescriptor.setRepository(this);
        descriptors.add(newDescriptor);

        IArtifactKey key = newDescriptor.getArtifactKey();
        Set<IArtifactDescriptor> keyDescriptors = descriptorsMap.get(key);
        if (keyDescriptors == null) {
            keyDescriptors = new HashSet<IArtifactDescriptor>();
            descriptorsMap.put(key, keyDescriptors);
        }
        keyDescriptors.add(newDescriptor);

        changedDescriptors.add(key);

        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw new ProvisionException("Could not create artifact file", e);
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
    public boolean contains(IArtifactDescriptor descriptor) {
        // TODO there should not be a descriptor if the file doesn't exist!
        return super.contains(descriptor) && getArtifactFile(descriptor).canRead();
    }

    @Override
    public void addDescriptor(IArtifactDescriptor descriptor) {
        super.addDescriptor(descriptor);

        changedDescriptors.add(descriptor.getArtifactKey());
    }

    @Override
    public void removeDescriptor(IArtifactDescriptor descriptor) {
        super.removeDescriptor(descriptor);

        IArtifactKey key = descriptor.getArtifactKey();

        Set<IArtifactDescriptor> keyDescriptors = descriptorsMap.get(key);

        if (keyDescriptors != null) {
            keyDescriptors.remove(descriptor);
            if (keyDescriptors.isEmpty()) {
                descriptorsMap.remove(key);
            }
        }

        descriptors.remove(descriptor);
        getArtifactFile(descriptor).delete();

        changedDescriptors.remove(descriptor.getArtifactKey());
        // TODO this doesn't work if the descriptor is not in changedDescriptors
        // TODO who needs this method?
    }

}
