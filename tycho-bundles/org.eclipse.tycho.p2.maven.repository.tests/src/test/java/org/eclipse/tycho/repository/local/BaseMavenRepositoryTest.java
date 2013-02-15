/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP AG and others.
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
import java.util.Properties;

import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenContextImpl;
import org.eclipse.tycho.p2.impl.repo.FileBasedTychoRepositoryIndex;
import org.eclipse.tycho.p2.impl.repo.LocalRepositoryP2IndicesImpl;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.NoopFileLockService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public abstract class BaseMavenRepositoryTest {

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();
    @Rule
    public final LogVerifier logVerifier = new LogVerifier();

    protected LocalRepositoryP2Indices localRepoIndices;
    protected File baseDir;

    @Before
    public void createLocalRepoIndices() {
        baseDir = tempFolder.newFolder("repository");
        MavenContext mavenContext = new MavenContextImpl(baseDir, false, logVerifier.getLogger(), new Properties());
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

}
