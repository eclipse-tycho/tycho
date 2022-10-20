/*******************************************************************************
 * Copyright (c) 2013 SAP SE and others.
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
package org.eclipse.tycho.test.util;

import java.io.File;
import java.io.IOException;

import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.tycho.p2.repository.LocalArtifactRepository;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2resolver.LocalRepositoryP2IndicesImpl;
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

    public LogVerifier logVerifier = new LogVerifier();
    private final TemporaryFolder tempManager = new TemporaryFolder();
    private File repoRoot;
    private LocalRepositoryP2IndicesImpl repoIndex;
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
            repoIndex = new LocalRepositoryP2IndicesImpl();
            repoIndex.setFileLockService(new NoopFileLockService());
            repoIndex.setMavenContext(new MockMavenContext(getLocalRepositoryRoot(), logVerifier.getLogger()));
        }
        return repoIndex;
    }

    public LocalArtifactRepository getLocalArtifactRepository() {
        if (repo == null) {
            repo = new LocalArtifactRepository(null, getLocalRepositoryIndex());
        }
        return repo;
    }
}
