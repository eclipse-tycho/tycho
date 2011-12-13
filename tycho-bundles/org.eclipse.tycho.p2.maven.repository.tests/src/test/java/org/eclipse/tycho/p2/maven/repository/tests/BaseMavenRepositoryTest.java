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

package org.eclipse.tycho.p2.maven.repository.tests;

import java.io.File;

import org.eclipse.tycho.core.facade.MavenContextImpl;
import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.locking.facade.FileLocker;
import org.eclipse.tycho.locking.facade.LockTimeoutException;
import org.eclipse.tycho.p2.impl.repo.FileBasedTychoRepositoryIndex;
import org.eclipse.tycho.p2.impl.repo.LocalRepositoryP2IndicesImpl;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public abstract class BaseMavenRepositoryTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    protected LocalRepositoryP2Indices localRepoIndices;
    protected File baseDir;

    @Before
    public void createLocalRepoIndices() {
        baseDir = tempFolder.newFolder("repository");
        MavenContextImpl mavenContext = new MavenContextImpl();
        mavenContext.setLocalRepositoryRoot(baseDir);
        LocalRepositoryP2IndicesImpl tempLocalRepoIndices = new LocalRepositoryP2IndicesImpl();
        tempLocalRepoIndices.setMavenContext(mavenContext);
        tempLocalRepoIndices.setFileLockService(new NoopFileLockService());
        this.localRepoIndices = tempLocalRepoIndices;
    }

    protected TychoRepositoryIndex createArtifactsIndex(File location) {
        return FileBasedTychoRepositoryIndex.createArtifactsIndex(location, new NoopFileLockService());
    }

    protected TychoRepositoryIndex createMetadataIndex(File location) {
        return FileBasedTychoRepositoryIndex.createMetadataIndex(location, new NoopFileLockService());
    }

    private static class NoopFileLockService implements FileLockService {

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.tycho.locking.facade.FileLockService#getFileLocker(java.io.File)
         */
        public FileLocker getFileLocker(File file) {
            return new FileLocker() {

                public void release() {
                }

                public void lock(long timeout) throws LockTimeoutException {
                }

                public void lock() throws LockTimeoutException {
                }

                public boolean isLocked() {
                    return false;
                }
            };
        }

    }
}
