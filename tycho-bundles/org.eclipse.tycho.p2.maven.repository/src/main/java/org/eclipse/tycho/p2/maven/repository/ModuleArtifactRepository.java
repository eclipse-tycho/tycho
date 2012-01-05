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
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.p2.maven.repository.xmlio.ArtifactsIO;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;

/**
 * Exposes a module's build target directory as p2 artifact repository. Instances are based on the
 * following data sources:
 * <ul>
 * <li>The <code>p2artifacts.xml</code> file in the given build target directory, which contains a
 * list of all artifacts with p2 <i>and</i> Maven coordinates. (In particular the classifier part of
 * the Maven coordinates is relevant.)</li>
 * <li>The <code>local-artifacts.properties</code> file, which maps classifiers to the file system
 * locations of the artifacts <i>before</i> they are uploaded into a Maven repository.</li>
 * </ul>
 * 
 * @see RepositoryLayoutHelper#FILE_NAME_P2_ARTIFACTS
 * @see RepositoryLayoutHelper#FILE_NAME_LOCAL_ARTIFACTS
 */
public class ModuleArtifactRepository extends AbstractMavenArtifactRepository {
    private static final GAV MODULE_GAV = new GAV("dummy-groupId", "dummy-artifactId", "dummy-version");

    public ModuleArtifactRepository(IProvisioningAgent agent, File repositoryDir) throws ProvisionException {
        super(agent, repositoryDir.toURI(), new ModuleArtifactReader(repositoryDir));
        load();
    }

    @Override
    public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
        throw new UnsupportedOperationException();
    }

    private void load() throws ProvisionException {
        File p2DataFile = contentLocator.getLocalArtifactLocation(MODULE_GAV,
                RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS, RepositoryLayoutHelper.EXTENSION_P2_ARTIFACTS);

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

    static boolean canAttemptRead(File repositoryDir) {
        File requiredP2ArtifactsFile = new File(repositoryDir, RepositoryLayoutHelper.FILE_NAME_P2_ARTIFACTS);
        File requiredLocalArtifactsFile = new File(repositoryDir, RepositoryLayoutHelper.FILE_NAME_LOCAL_ARTIFACTS);
        return requiredP2ArtifactsFile.isFile() && requiredLocalArtifactsFile.isFile();
    }

}
