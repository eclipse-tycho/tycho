/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.maven.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.tycho.p2.maven.repository.xmlio.ArtifactsIO;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;

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
public class ModuleArtifactRepository extends AbstractMavenArtifactRepository {
    /**
     * Type string for this repository type. This value needs to be passed to
     * {@link IArtifactRepositoryManager#createRepository(URI, String, String, Map)} in order to
     * create a repository of type {@link ModuleArtifactRepository}.
     */
    // must match the extension point id of ModuleArtifactRepositoryFactory; should be the qualified class name
    public static final String REPOSITORY_TYPE = "org.eclipse.tycho.p2.maven.repository.ModuleArtifactRepository";

    private static final GAV DUMMY_GAV = null;

    private final File p2DataFile;

    private final ModuleArtifactReader artifactsMap;

    // BEGIN construction

    public static ModuleArtifactRepository restoreInstance(IProvisioningAgent agent, File repositoryDir)
            throws ProvisionException {
        ModuleArtifactRepository restoredInstance = new ModuleArtifactRepository(agent, repositoryDir.toURI(),
                ModuleArtifactReader.restoreInstance(repositoryDir));

        restoredInstance.load();
        return restoredInstance;
    }

    public static ModuleArtifactRepository createInstance(IProvisioningAgent agent, File repositoryDir)
            throws ProvisionException {
        ModuleArtifactRepository newInstance = new ModuleArtifactRepository(agent, repositoryDir.toURI(),
                createArtifactLocationMap(repositoryDir));

        // make sure p2artifacts.xml exists
        newInstance.storeOrProvisioningException();
        return newInstance;
    }

    private static ModuleArtifactReader createArtifactLocationMap(File repositoryDir) throws ProvisionException {
        ModuleArtifactReader artifactLocationMap = ModuleArtifactReader.createInstance(repositoryDir);

        // add p2artifacts.xml in standard location
        artifactLocationMap.add(RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS, new File(repositoryDir,
                RepositoryLayoutHelper.FILE_NAME_P2_ARTIFACTS));
        return artifactLocationMap;
    }

    private ModuleArtifactRepository(IProvisioningAgent agent, URI uri, ModuleArtifactReader artifactsMap) {
        super(agent, uri, artifactsMap);
        this.artifactsMap = artifactsMap;
        this.p2DataFile = contentLocator.getLocalArtifactLocation(DUMMY_GAV,
                RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS, RepositoryLayoutHelper.EXTENSION_P2_ARTIFACTS);
    }

    // END construction

    @Override
    public IArtifactDescriptor createArtifactDescriptor(IArtifactKey key) {
        ArtifactDescriptor result = new ArtifactDescriptor(key);
        result.setRepository(this);

        // TODO 348586 use GAV from module
        result.setProperty(RepositoryLayoutHelper.PROP_GROUP_ID, "");
        result.setProperty(RepositoryLayoutHelper.PROP_ARTIFACT_ID, "");
        result.setProperty(RepositoryLayoutHelper.PROP_VERSION, "0.0.1");

        // TODO 348586 extract this to an associated object
        // guess classifier from the key
        if ("binary".equals(key.getClassifier())) { // TODO replace with org.eclipse.equinox.spi.p2.publisher.PublisherHelper.BINARY_ARTIFACT_CLASSIFIER
            // TODO strip product/feature id from this
            String mavenClassifier = key.getId();
            result.setProperty(RepositoryLayoutHelper.PROP_CLASSIFIER, mavenClassifier);
        } else {
            throw new IllegalArgumentException("Unexpected artifact: " + key);
        }

        return result;
    }

    @Override
    public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
        // TODO check that this is a descriptor created by us, e.g. through sub-classing ArtifactDescriptor

        String classifier = descriptor.getProperty(RepositoryLayoutHelper.PROP_CLASSIFIER);

        // look up storage location
        File storageLocation = artifactsMap.addToAutomaticLocation(classifier);

        // TODO download.size/artifact.size is not set in the descriptor for product binaries -> compute while streaming  

        internalAddDescriptor(descriptor);
        storeOrProvisioningException();

        try {
            return new FileOutputStream(storageLocation);
        } catch (FileNotFoundException e) {
            throw new ProvisionException("Failed to write artifact " + descriptor.getArtifactKey() + " to "
                    + storageLocation, e);
        }
    }

    private void load() throws ProvisionException {
        try {
            ArtifactsIO io = new ArtifactsIO();
            Set<IArtifactDescriptor> initialDescriptors = io.readXML(new FileInputStream(p2DataFile));
            for (IArtifactDescriptor descriptor : initialDescriptors) {
                internalAddDescriptor(descriptor);
            }
        } catch (IOException e) {
            String message = "Error while reading repository from " + p2DataFile;
            int code = ProvisionException.REPOSITORY_FAILED_READ;
            Status status = new Status(IStatus.ERROR, Activator.ID, code, message, e);
            throw new ProvisionException(status);
        }
    }

    private void storeOrProvisioningException() throws ProvisionException {
        try {
            internalStore();
        } catch (IOException e) {
            String message = "Error while writing repository to " + p2DataFile;
            int code = ProvisionException.REPOSITORY_FAILED_WRITE;
            Status status = new Status(IStatus.ERROR, Activator.ID, code, message, e);
            throw new ProvisionException(status);
        }
    }

    @Override
    protected void store() {
        try {
            internalStore();
        } catch (IOException e) {
            String message = "Error while writing repository to " + p2DataFile;
            throw new RuntimeException(message, e);
        }
    }

    private void internalStore() throws IOException {
        // store without exception handling
        ArtifactsIO io = new ArtifactsIO();
        io.writeXML(descriptors, p2DataFile);
    }

    static boolean canAttemptRead(File repositoryDir) {
        File requiredP2ArtifactsFile = new File(repositoryDir, RepositoryLayoutHelper.FILE_NAME_P2_ARTIFACTS);
        File requiredLocalArtifactsFile = new File(repositoryDir, RepositoryLayoutHelper.FILE_NAME_LOCAL_ARTIFACTS);
        return requiredP2ArtifactsFile.isFile() && requiredLocalArtifactsFile.isFile();
    }

}
