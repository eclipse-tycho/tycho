/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.local.testutil;

import java.io.File;
import java.io.IOException;

import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.repository.local.LocalArtifactRepository;
import org.eclipse.tycho.repository.local.index.LocalRepositoryP2IndicesImpl;
import org.eclipse.tycho.test.util.NoopFileLockService;
import org.junit.Rule;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;

/**
 * JUnit {@link Rule} that can provide a {@link LocalArtifactRepository} for a temporary local Maven
 * repository directory, or other objects needed for testing an {@link LocalArtifactRepository}
 * instance.
 */
@SuppressWarnings("restriction")
public class TemporaryLocalMavenRepository extends ExternalResource {
    private final TemporaryFolder tempManager = new TemporaryFolder();
    private File repoRoot;
    private LocalRepositoryP2Indices repoIndex;
    private LocalArtifactRepository repo;

    @Override
    protected void before() throws Throwable {
        tempManager.create();
    }

    @Override
    protected void after() {
        tempManager.delete();
    }

    public void initContentFromResourceFolder(File resourceFolder) throws IOException {
        FileUtils.copy(resourceFolder, getLocalRepositoryRoot(), new File("."), true);
    }

    public File getLocalRepositoryRoot() {
        if (repoRoot == null) {
            try {
                repoRoot = tempManager.newFolder("repository");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return repoRoot;
    }

    public LocalRepositoryP2Indices getLocalRepositoryIndex() {
        if (repoIndex == null) {
            createLocalRepoIndices();
        }
        return repoIndex;
    }

    private void createLocalRepoIndices() {
        repoIndex = new LocalRepositoryP2IndicesImpl(getLocalRepositoryRoot(), new NoopFileLockService());
    }

    public LocalArtifactRepository getLocalArtifactRepository() {
        if (repo == null) {
            repo = new LocalArtifactRepository(null, getLocalRepositoryIndex());
        }
        return repo;
    }
}
