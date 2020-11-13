/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP SE and others.
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
package org.eclipse.tycho.repository.module;

import static java.util.stream.Collectors.toSet;
import static org.eclipse.tycho.repository.util.internal.BundleConstants.BUNDLE_ID;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.tycho.p2.maven.repository.xmlio.ArtifactsIO;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.MavenRepositoryCoordinates;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.repository.module.ModuleArtifactRepository.ModuleArtifactDescriptor;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicies;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IArtifactSink;
import org.eclipse.tycho.repository.p2base.artifact.repository.ArtifactRepositoryBaseImpl;
import org.eclipse.tycho.repository.publishing.WriteSessionContext;
import org.eclipse.tycho.repository.publishing.WriteSessionContext.ClassifierAndExtension;
import org.eclipse.tycho.repository.util.GAVArtifactDescriptorBase;

/**
 * A p2 artifact repository implementation for the build output directory. Instances are persisted
 * in the following files:
 * <ul>
 * <li>A <tt>p2artifacts.xml</tt> file in the given build target directory, which contains a list of
 * all artifacts with p2 <i>and</i> Maven coordinates. (In particular the classifier part of the
 * Maven coordinates is relevant.) This file is deployed to Maven repositories alongside with the
 * built Tycho artifact.</li>
 * <li>The <code>local-artifacts.properties</code> file, which maps classifiers to the file system
 * locations of the artifacts <i>before</i> they are uploaded into a Maven repository. (Unlike in a
 * Maven repository, there are no predefined locations for the artifacts in the build output
 * directory.)</li>
 * </ul>
 * 
 * @see RepositoryLayoutHelper#FILE_NAME_P2_ARTIFACTS
 * @see RepositoryLayoutHelper#FILE_NAME_LOCAL_ARTIFACTS
 */
class ModuleArtifactRepository extends ArtifactRepositoryBaseImpl<ModuleArtifactDescriptor> {
    /**
     * Type string for this repository type. This value needs to be passed to
     * {@link IArtifactRepositoryManager#createRepository(URI, String, String, Map)} in order to
     * create a repository of type {@link ModuleArtifactRepository}.
     */
    // must match the extension point id of ModuleArtifactRepositoryFactory; should be the qualified class name
    public static final String REPOSITORY_TYPE = "org.eclipse.tycho.repository.module.ModuleArtifactRepository";

    private static final GAV DUMMY_GAV = null;

    private final File p2DataFile;

    private final ModuleArtifactMap artifactsMap;

    private GAV moduleGAV;

    // BEGIN construction

    static boolean canAttemptRead(File repositoryDir) {
        File requiredP2ArtifactsFile = new File(repositoryDir, RepositoryLayoutHelper.FILE_NAME_P2_ARTIFACTS);
        File requiredLocalArtifactsFile = new File(repositoryDir, RepositoryLayoutHelper.FILE_NAME_LOCAL_ARTIFACTS);
        return requiredP2ArtifactsFile.isFile() && requiredLocalArtifactsFile.isFile();
    }

    public static ModuleArtifactRepository restoreInstance(IProvisioningAgent agent, File repositoryDir)
            throws ProvisionException {
        ModuleArtifactRepository restoredInstance = new ModuleArtifactRepository(agent, repositoryDir,
                ModuleArtifactMap.restoreInstance(repositoryDir));

        restoredInstance.load();
        return restoredInstance;
    }

    public static ModuleArtifactRepository createInstance(IProvisioningAgent agent, File repositoryDir)
            throws ProvisionException {
        ModuleArtifactRepository newInstance = new ModuleArtifactRepository(agent, repositoryDir,
                createArtifactLocationMap(repositoryDir));

        // make sure p2artifacts.xml exists
        newInstance.storeOrProvisioningException();
        return newInstance;
    }

    private static ModuleArtifactMap createArtifactLocationMap(File repositoryDir) throws ProvisionException {
        ModuleArtifactMap artifactLocationMap = ModuleArtifactMap.createInstance(repositoryDir);

        // add p2artifacts.xml in standard location
        artifactLocationMap.add(RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS,
                new File(repositoryDir, RepositoryLayoutHelper.FILE_NAME_P2_ARTIFACTS));
        return artifactLocationMap;
    }

    private ModuleArtifactRepository(IProvisioningAgent agent, File location, ModuleArtifactMap artifactsMap) {
        super(agent, location.toURI(), ArtifactTransferPolicies.forLocalArtifacts());
        this.artifactsMap = artifactsMap;

        this.p2DataFile = artifactsMap.getLocalArtifactLocation(new MavenRepositoryCoordinates(DUMMY_GAV,
                RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS, RepositoryLayoutHelper.EXTENSION_P2_ARTIFACTS));
    }

    // TODO the GAV should not be mutable; it should be encoded in the location URI
    public void setGAV(String groupId, String artifactId, String version) {
        this.moduleGAV = new GAV(groupId, artifactId, version);
    }

    // END construction

    public ModuleArtifactMap getArtifactsMap() {
        return artifactsMap;
    }

    @Override
    protected File internalGetArtifactStorageLocation(IArtifactDescriptor descriptor) {
        return artifactsMap.getLocalArtifactLocation(readMavenCoordinates(descriptor));
    }

    private static MavenRepositoryCoordinates readMavenCoordinates(IArtifactDescriptor descriptor) {
        if (descriptor instanceof ModuleArtifactDescriptor) {
            return ((ModuleArtifactDescriptor) descriptor).getMavenCoordinates();

        } else {
            MavenRepositoryCoordinates result = GAVArtifactDescriptorBase.readMavenCoordinateProperties(descriptor);
            if (result == null) {
                throw new IllegalArgumentException(
                        "Maven coordinate properties are missing in artifact descriptor " + descriptor);
            }
            return result;
        }
    }

    @Override
    protected IArtifactDescriptor getComparableDescriptor(IArtifactDescriptor descriptor) {
        if (descriptor instanceof ModuleArtifactDescriptor) {
            return descriptor;
        } else {
            // convert to type that may be equal to an internal ModuleArtifactDescriptor
            return new ModuleArtifactComparableDescriptor(descriptor);
        }
    }

    @Override
    protected ModuleArtifactDescriptor getInternalDescriptorForAdding(IArtifactDescriptor descriptor)
            throws IllegalArgumentException {
        if (descriptor == null) {
            throw new NullPointerException();
        } else if (!(descriptor instanceof ModuleArtifactDescriptor) || descriptor.getRepository() != this) {
            throw new IllegalArgumentException(
                    "Cannot add artifact descriptor which has not been created by this repository");
        }
        ModuleArtifactDescriptor internalDescriptor = (ModuleArtifactDescriptor) descriptor;

        try {
            MavenRepositoryCoordinates coordinates = internalDescriptor.getMavenCoordinates();
            artifactsMap.addToAutomaticLocation(coordinates.getClassifier(), coordinates.getExtension());

        } catch (ProvisionException e) {
            // TODO 393004 Revise exception handling
            throw new RuntimeException(e);
        }
        // TODO only persist when committing new artifact?

        return internalDescriptor;
    }

    // we need to know the classifier when create new artifact descriptors
    @Override
    public ModuleArtifactDescriptor createArtifactDescriptor(IArtifactKey key) {
        throw new UnsupportedOperationException();
    }

    public IArtifactDescriptor createArtifactDescriptor(IArtifactKey key, WriteSessionContext writeSession) {
        ClassifierAndExtension additionalProperties = writeSession.getClassifierAndExtensionForNewKey(key);
        MavenRepositoryCoordinates mavenCoordinates = new MavenRepositoryCoordinates(moduleGAV,
                additionalProperties.classifier, additionalProperties.fileExtension);

        return new ModuleArtifactDescriptor(key, mavenCoordinates);
    }

    // TODO use this method from ModuleArtifactRepositoryDelegate?
    public IArtifactSink newAddingArtifactSink(IArtifactKey key, WriteSessionContext writeSession)
            throws ProvisionException {
        ModuleArtifactDescriptor internalDescriptorForAdding = getInternalDescriptorForAdding(
                createArtifactDescriptor(key, writeSession));
        return internalNewAddingArtifactSink(internalDescriptorForAdding);
    }

    @Override
    protected void internalStore(IProgressMonitor monitor) {
        try {
            internalStoreWithException();
        } catch (IOException e) {
            String message = "Error while writing repository to " + p2DataFile;
            // TODO 393004 Use a specific type?
            throw new RuntimeException(message, e);
        }
    }

    private void storeOrProvisioningException() throws ProvisionException {
        try {
            internalStoreWithException();
        } catch (IOException e) {
            String message = "Error while writing repository to " + p2DataFile;
            int code = ProvisionException.REPOSITORY_FAILED_WRITE;
            Status status = new Status(IStatus.ERROR, BUNDLE_ID, code, message, e);
            throw new ProvisionException(status);
        }
    }

    private void internalStoreWithException() throws IOException {
        ArtifactsIO io = new ArtifactsIO();
        io.writeXML(flattenedValues().collect(toSet()), p2DataFile);
    }

    private void load() throws ProvisionException {
        try (FileInputStream p2DataFileStream = new FileInputStream(p2DataFile)) {
            Set<IArtifactDescriptor> descriptors = new ArtifactsIO().readXML(p2DataFileStream);
            for (IArtifactDescriptor descriptor : descriptors) {
                ModuleArtifactDescriptor internalDescriptor = getInternalDescriptorFromLoadedDescriptor(descriptor,
                        p2DataFile);
                // TODO check that GAV properties match module GAV
                internalAddInternalDescriptor(internalDescriptor);
            }
        } catch (IOException e) {
            throw failedReadException(p2DataFile, null, e);
        }
    }

    private ModuleArtifactDescriptor getInternalDescriptorFromLoadedDescriptor(IArtifactDescriptor loadedDescriptor,
            File sourceFile) throws ProvisionException {
        MavenRepositoryCoordinates mavenCoordinates = GAVArtifactDescriptorBase
                .readMavenCoordinateProperties(loadedDescriptor);
        if (mavenCoordinates != null) {
            return new ModuleArtifactDescriptor(loadedDescriptor, mavenCoordinates);

        } else {
            throw failedReadException(sourceFile,
                    "Maven coordinate properties are missing in artifact descriptor " + loadedDescriptor, null);
        }
    }

    static ProvisionException failedReadException(File sourceFile, String details, Exception exception) {
        String message = "Error while reading repository from " + sourceFile;
        if (details != null) {
            message += ": " + details;
        }
        int code = ProvisionException.REPOSITORY_FAILED_READ;
        Status status = new Status(IStatus.ERROR, BUNDLE_ID, code, message, exception);
        return new ProvisionException(status);
    }

    @Override
    public boolean isModifiable() {
        return true;
    }

    /**
     * An artifact descriptor with Maven coordinates. Only descriptors of this type can be stored in
     * a {@link ModuleArtifactRepository}. The Maven coordinates are determined by the module that
     * is being built &ndash; see
     * {@link ModuleArtifactRepository#createArtifactDescriptor(IArtifactKey, WriteSessionContext)}.
     */
    class ModuleArtifactDescriptor extends GAVArtifactDescriptorBase {

        ModuleArtifactDescriptor(IArtifactDescriptor base, MavenRepositoryCoordinates mavenCoordinates) {
            super(base, mavenCoordinates, false); // Maven coordinates are copied from the base
        }

        ModuleArtifactDescriptor(IArtifactKey p2Key, MavenRepositoryCoordinates mavenCoordinates) {
            super(p2Key, mavenCoordinates, true); // set Maven coordinate properties
        }

        @Override
        public IArtifactRepository getRepository() {
            return ModuleArtifactRepository.this;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (!(obj instanceof IArtifactDescriptor))
                return false;
            IArtifactDescriptor other = (IArtifactDescriptor) obj;

            if (other instanceof ModuleArtifactDescriptor || other instanceof ModuleArtifactComparableDescriptor) {
                // compare fields used in ArtifactDescriptor.hashCode
                return Objects.equals(this.key, other.getArtifactKey())
                        && Objects.equals(this.getProperty(FORMAT), other.getProperty(FORMAT))
                        && Arrays.equals(this.processingSteps, other.getProcessingSteps());
            }
            return false;
        }
    }

    /**
     * An artifact descriptor that can be compared with {@link ModuleArtifactDescriptor}. Unlike the
     * latter, this type does not need to have Maven coordinate properties. (The Maven coordinates
     * are not relevant for equals and hashCode.)
     */
    private static class ModuleArtifactComparableDescriptor extends ArtifactDescriptor {

        public ModuleArtifactComparableDescriptor(IArtifactDescriptor descriptor) {
            super(descriptor);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof IArtifactDescriptor))
                return false;
            IArtifactDescriptor other = (IArtifactDescriptor) obj;

            if (other instanceof ModuleArtifactDescriptor || other instanceof ModuleArtifactComparableDescriptor) {
                // compare fields used in ArtifactDescriptor.hashCode
                return Objects.equals(this.key, other.getArtifactKey())
                        && Objects.equals(this.getProperty(FORMAT), other.getProperty(FORMAT))
                        && Arrays.equals(this.processingSteps, other.getProcessingSteps());
            }
            return false;
        }

    }

    @Override
    public boolean isFileAlreadyAvailable(IArtifactKey artifactKey) {
        return contains(artifactKey);
    }

}
