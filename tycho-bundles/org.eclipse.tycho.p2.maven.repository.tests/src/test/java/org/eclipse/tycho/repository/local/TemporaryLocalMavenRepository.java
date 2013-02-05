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
package org.eclipse.tycho.repository.local;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenContextImpl;
import org.eclipse.tycho.p2.impl.repo.LocalRepositoryP2IndicesImpl;
import org.eclipse.tycho.p2.maven.repository.tests.ResourceUtil;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.test.util.MemoryLog;
import org.eclipse.tycho.test.util.NoopFileLockService;
import org.junit.Rule;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;

/**
 * JUnit {@link Rule} that can provide a {@link LocalArtifactRepository} for a temporary local Maven
 * repository directory, or other objects needed for testing an {@link LocalArtifactRepository}
 * instance.
 */
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

    @SuppressWarnings("restriction")
    public void initContentFromTestResource(String path) throws IOException {
        FileUtils.copy(ResourceUtil.resourceFile(path), getLocalRepositoryRoot(), new File("."), true);
    }

    public File getLocalRepositoryRoot() {
        if (repoRoot == null) {
            repoRoot = tempManager.newFolder("repository");
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
        MavenContext mavenContext = new MavenContextImpl(getLocalRepositoryRoot(), false, new MemoryLog(),
                new Properties());

        repoIndex = new LocalRepositoryP2IndicesImpl(mavenContext, new NoopFileLockService());
    }

    public LocalArtifactRepository getLocalArtifactRepository() {
        if (repo == null) {
            repo = new LocalArtifactRepository(null, getLocalRepositoryIndex());
        }
        return repo;
    }
}
