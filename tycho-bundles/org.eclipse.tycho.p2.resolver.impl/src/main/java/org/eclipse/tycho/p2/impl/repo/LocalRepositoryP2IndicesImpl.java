/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.p2.impl.repo;

import java.io.File;

import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;

public class LocalRepositoryP2IndicesImpl implements LocalRepositoryP2Indices {

    // collaborators
    private MavenContext mavenContext;
    private FileLockService fileLockService;

    // derived members
    private boolean initialized = false;
    private TychoRepositoryIndex artifactsIndex;
    private TychoRepositoryIndex metadataIndex;

    // constructor for DS
    public LocalRepositoryP2IndicesImpl() {
    }

    // injected by DS runtime
    public void setMavenContext(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
    }

    // injected by DS runtime
    public void setFileLockService(FileLockService fileLockService) {
        this.fileLockService = fileLockService;
    }

    // test constructor
    public LocalRepositoryP2IndicesImpl(MavenContext mavenContext, FileLockService fileLockService) {
        this.mavenContext = mavenContext;
        this.fileLockService = fileLockService;
    }

    private void checkInitialized() {
        if (initialized) {
            return;
        }
        File localRepositoryRoot = mavenContext.getLocalRepositoryRoot();
        this.artifactsIndex = FileBasedTychoRepositoryIndex.createArtifactsIndex(localRepositoryRoot, fileLockService);
        this.metadataIndex = FileBasedTychoRepositoryIndex.createMetadataIndex(localRepositoryRoot, fileLockService);
        initialized = true;
    }

    public TychoRepositoryIndex getArtifactsIndex() {
        checkInitialized();
        return artifactsIndex;
    }

    public TychoRepositoryIndex getMetadataIndex() {
        checkInitialized();
        return metadataIndex;
    }

    public File getBasedir() {
        return mavenContext.getLocalRepositoryRoot();
    }

}
