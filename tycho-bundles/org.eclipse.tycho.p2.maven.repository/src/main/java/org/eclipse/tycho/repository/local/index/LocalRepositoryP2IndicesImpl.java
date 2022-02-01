/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.repository.local.index;

import java.io.File;

import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;

public class LocalRepositoryP2IndicesImpl implements LocalRepositoryP2Indices {

    // injected members
    private FileLockService fileLockService;
    private File localRepositoryRoot;
    private MavenLogger logger;

    // derived members
    private boolean initialized = false;
    private TychoRepositoryIndex artifactsIndex;
    private TychoRepositoryIndex metadataIndex;
    private MavenContext mavenContext;

    // constructor for DS
    public LocalRepositoryP2IndicesImpl() {
    }

    // test constructor
    public LocalRepositoryP2IndicesImpl(MavenContext mavenContext, FileLockService fileLockService) {
        this.localRepositoryRoot = mavenContext.getLocalRepositoryRoot();
        this.mavenContext = mavenContext;
        this.fileLockService = fileLockService;
    }

    // injected by DS runtime
    public void setMavenContext(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
        this.localRepositoryRoot = mavenContext.getLocalRepositoryRoot();
        this.logger = mavenContext.getLogger();
    }

    // injected by DS runtime
    public void setFileLockService(FileLockService fileLockService) {
        this.fileLockService = fileLockService;
    }

    private void checkInitialized() {
        if (initialized) {
            return;
        }
        this.artifactsIndex = FileBasedTychoRepositoryIndex.createArtifactsIndex(localRepositoryRoot, fileLockService,
                mavenContext);
        this.metadataIndex = FileBasedTychoRepositoryIndex.createMetadataIndex(localRepositoryRoot, fileLockService,
                mavenContext);
        initialized = true;
    }

    @Override
    public TychoRepositoryIndex getArtifactsIndex() {
        checkInitialized();
        return artifactsIndex;
    }

    @Override
    public TychoRepositoryIndex getMetadataIndex() {
        checkInitialized();
        return metadataIndex;
    }

    @Override
    public File getBasedir() {
        return localRepositoryRoot;
    }

    @Override
    public MavenContext getMavenContext() {
        return mavenContext;
    }

}
