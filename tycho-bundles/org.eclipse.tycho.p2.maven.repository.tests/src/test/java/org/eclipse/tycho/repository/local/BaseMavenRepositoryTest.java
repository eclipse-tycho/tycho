/*******************************************************************************
 * Copyright (c) 2011, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.repository.local;

import java.io.File;

import org.eclipse.tycho.p2.impl.repo.FileBasedTychoRepositoryIndex;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;
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
        return FileBasedTychoRepositoryIndex.createArtifactsIndex(location, new NoopFileLockService());
    }

    final TychoRepositoryIndex createMetadataIndex(File location) {
        return FileBasedTychoRepositoryIndex.createMetadataIndex(location, new NoopFileLockService());
    }

}
