/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
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
import java.io.IOException;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.tycho.p2.repository.AbstractRepositoryReader;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;

/**
 * Exposes a module's build target directory as p2 metadata repository. The data source is the
 * metadata file produced by the p2-metadata goal.
 * 
 * @see RepositoryLayoutHelper#FILE_NAME_P2_METADATA
 */
public class ModuleMetadataRepository extends AbstractMavenMetadataRepository {

    public ModuleMetadataRepository(IProvisioningAgent agent, File location) {
        super(agent, location.toURI(), createDummyIndex(), new ModuleMetadataReader(location));
    }

    private static TychoRepositoryIndex createDummyIndex() {
        return new MemoryTychoRepositoryIndex(new GAV("g", "a", "v"));
    }

    private static class ModuleMetadataReader extends AbstractRepositoryReader {
        private final File repositoryDir;

        public ModuleMetadataReader(File location) {
            this.repositoryDir = location;
        }

        public File getLocalArtifactLocation(GAV gav, String classifier, String extension) throws IOException {
            if (RepositoryLayoutHelper.CLASSIFIER_P2_METADATA.equals(classifier)) {
                return new File(repositoryDir, RepositoryLayoutHelper.FILE_NAME_P2_METADATA);
            } else {
                // AbstractMavenMetadataRepository doesn't call this method for anything other than the p2metadata artifact
                throw new IllegalArgumentException();
            }
        }

    }// end nested class

    static boolean canAttemptRead(File repositoryDir) {
        File requiredP2MetadataFile = new File(repositoryDir, RepositoryLayoutHelper.FILE_NAME_P2_METADATA);
        return requiredP2MetadataFile.isFile();
    }
}
