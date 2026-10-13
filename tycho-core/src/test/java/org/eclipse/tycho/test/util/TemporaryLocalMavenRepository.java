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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.tycho.p2.repository.LocalArtifactRepository;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2resolver.LocalRepositoryP2IndicesImpl;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Extension that can provide a {@link LocalArtifactRepository} for a temporary local Maven
 * repository directory, or other objects needed for testing an {@link LocalArtifactRepository}
 * instance.
 */
public class TemporaryLocalMavenRepository implements BeforeEachCallback, AfterEachCallback {

    // not registered as an extension, only used to hand a Logger to the MockMavenContext below
    public LogVerifier logVerifier = new LogVerifier();
    private Path tempRoot;
    private File repoRoot;
    private LocalRepositoryP2IndicesImpl repoIndex;
    private LocalArtifactRepository repo;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        tempRoot = Files.createTempDirectory("tycho-local-maven-repo");
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        repo = null;
        repoIndex = null;
        repoRoot = null;
        if (tempRoot != null) {
            try (Stream<Path> paths = Files.walk(tempRoot)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            } finally {
                tempRoot = null;
            }
        }
    }

    public void initContentFromResourceFolder(File resourceFolder) throws IOException {
        FileUtils.copy(resourceFolder, getLocalRepositoryRoot(), new File("."), true);
    }

    public File getLocalRepositoryRoot() {
        if (repoRoot == null) {
            try {
                repoRoot = Files.createDirectories(tempRoot.resolve("repository")).toFile();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
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
