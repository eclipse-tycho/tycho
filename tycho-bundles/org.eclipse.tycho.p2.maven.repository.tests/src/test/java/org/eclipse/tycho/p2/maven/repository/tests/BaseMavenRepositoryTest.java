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

import org.eclipse.tycho.p2.impl.MavenContextImpl;
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
        this.localRepoIndices = tempLocalRepoIndices;
    }

    protected TychoRepositoryIndex createArtifactsIndex(File location) {
        return FileBasedTychoRepositoryIndex.createArtifactsIndex(location);
    }

    protected TychoRepositoryIndex createMetadataIndex(File location) {
        return FileBasedTychoRepositoryIndex.createMetadataIndex(location);
    }

}
