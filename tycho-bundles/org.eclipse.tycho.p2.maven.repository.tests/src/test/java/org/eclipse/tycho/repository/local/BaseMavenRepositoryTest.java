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
import java.io.IOException;
import java.util.Properties;

import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenContextImpl;
import org.eclipse.tycho.p2.impl.repo.FileBasedTychoRepositoryIndex;
import org.eclipse.tycho.p2.impl.repo.LocalRepositoryP2IndicesImpl;
import org.eclipse.tycho.p2.maven.repository.tests.ResourceUtil;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;
import org.eclipse.tycho.test.util.MemoryLog;
import org.eclipse.tycho.test.util.NoopFileLockService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("restriction")
public abstract class BaseMavenRepositoryTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    protected LocalRepositoryP2Indices localRepoIndices;
    protected File baseDir;

    @Before
    public void createLocalRepoIndices() {
        baseDir = tempFolder.newFolder("repository");
        MavenContext mavenContext = new MavenContextImpl(baseDir, false, new MemoryLog(), new Properties());
        LocalRepositoryP2IndicesImpl tempLocalRepoIndices = new LocalRepositoryP2IndicesImpl();
        tempLocalRepoIndices.setMavenContext(mavenContext);
        tempLocalRepoIndices.setFileLockService(new NoopFileLockService());
        this.localRepoIndices = tempLocalRepoIndices;
    }

    final TychoRepositoryIndex createArtifactsIndex(File location) {
        return FileBasedTychoRepositoryIndex.createArtifactsIndex(location, new NoopFileLockService());
    }

    final TychoRepositoryIndex createMetadataIndex(File location) {
        return FileBasedTychoRepositoryIndex.createMetadataIndex(location, new NoopFileLockService());
    }

    final void initLocalRepositoryFromTestResource(String path) throws IOException {
        FileUtils.copy(ResourceUtil.resourceFile(path), localRepoIndices.getBasedir(), new File("."), true);
    }

}
