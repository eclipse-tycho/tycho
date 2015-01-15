/*******************************************************************************
 * Copyright (c) 2011, 2013 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.repository.local;

import java.io.File;

import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;
import org.eclipse.tycho.repository.local.index.FileBasedTychoRepositoryIndex;
import org.eclipse.tycho.repository.local.testutil.TemporaryLocalMavenRepository;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.NoopFileLockService;
import org.junit.Before;
import org.junit.Rule;

/**
 * @deprecated Use {@link TemporaryLocalMavenRepository} rule instead of inheriting from this class.
 */
@Deprecated
public abstract class BaseMavenRepositoryTest {

    @Rule
    public TemporaryLocalMavenRepository tempLocalMavenRepository = new TemporaryLocalMavenRepository();
    @Rule
    public final LogVerifier logVerifier = new LogVerifier();

    protected LocalRepositoryP2Indices localRepoIndices;
    protected File baseDir;

    @Before
    public void initAliases() {
        baseDir = tempLocalMavenRepository.getLocalRepositoryRoot();
        localRepoIndices = tempLocalMavenRepository.getLocalRepositoryIndex();
    }

    final TychoRepositoryIndex createArtifactsIndex(File location) {
        return FileBasedTychoRepositoryIndex.createArtifactsIndex(location, new NoopFileLockService(),
                new NoopMavenLogger());
    }

    final TychoRepositoryIndex createMetadataIndex(File location) {
        return FileBasedTychoRepositoryIndex.createMetadataIndex(location, new NoopFileLockService(),
                new NoopMavenLogger());
    }

    static class NoopMavenLogger implements MavenLogger {

        @Override
        public void error(String message) {
        }

        @Override
        public void warn(String message) {
        }

        @Override
        public void warn(String message, Throwable cause) {
        }

        @Override
        public void info(String message) {
        }

        @Override
        public void debug(String message) {
        }

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public boolean isExtendedDebugEnabled() {
            return false;
        }

    }

}
