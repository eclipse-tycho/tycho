/*******************************************************************************
 * Copyright (c) 2011, 2022 SAP SE and others.
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

package org.eclipse.tycho.p2resolver;

import java.io.File;
import java.io.IOException;

import org.eclipse.tycho.FileLockService;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.p2.repository.FileBasedTychoRepositoryIndex;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
public class LocalRepositoryP2IndicesImpl implements LocalRepositoryP2Indices {

    // injected members
    private final FileLockService fileLockService;
    private final MavenContext mavenContext;

    private File localRepositoryRoot;

    // derived members
    private boolean initialized = false;
    private TychoRepositoryIndex artifactsIndex;
    private TychoRepositoryIndex metadataIndex;

    @Inject
    public LocalRepositoryP2IndicesImpl(FileLockService fileLockService, MavenContext mavenContext) {
        this.fileLockService = fileLockService;
        this.mavenContext = mavenContext;
    }

    private void checkInitialized() {
        if (initialized) {
            return;
        }
        this.artifactsIndex = FileBasedTychoRepositoryIndex.createArtifactsIndex(getLocalRepositoryRoot(),
                fileLockService, mavenContext);
        this.metadataIndex = FileBasedTychoRepositoryIndex.createMetadataIndex(getLocalRepositoryRoot(),
                fileLockService, mavenContext);
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
        return getLocalRepositoryRoot();
    }

    @Override
    public MavenContext getMavenContext() {
        return mavenContext;
    }

    @Override
    public synchronized void add(GAV gav) throws IOException {
        TychoRepositoryIndex artifactsIndex = getArtifactsIndex();
        TychoRepositoryIndex metadataIndex = getMetadataIndex();
        addGavAndSave(gav, artifactsIndex);
        addGavAndSave(gav, metadataIndex);
    }

    private static void addGavAndSave(GAV gav, TychoRepositoryIndex index) throws IOException {
        index.addGav(gav);
        index.save();
    }

    public File getLocalRepositoryRoot() {
        if (localRepositoryRoot == null) {
            localRepositoryRoot = mavenContext.getLocalRepositoryRoot();
        }
        return localRepositoryRoot;
    }
}
