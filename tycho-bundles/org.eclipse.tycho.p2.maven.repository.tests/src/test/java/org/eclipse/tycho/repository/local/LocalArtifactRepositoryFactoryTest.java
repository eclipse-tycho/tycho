/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.local;

import java.net.URI;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.LocalArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.LocalArtifactRepositoryFactory;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.repository.local.testutil.TemporaryLocalMavenRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LocalArtifactRepositoryFactoryTest {

    @Rule
    public TemporaryLocalMavenRepository tempLocalMavenRepository = new TemporaryLocalMavenRepository();
    private LocalArtifactRepositoryFactory subject;

    @Before
    public void setUp() {
        subject = new LocalArtifactRepositoryFactory() {

            @Override
            protected LocalRepositoryP2Indices lookupLocalRepoIndices() {
                return tempLocalMavenRepository.getLocalRepositoryIndex();
            }
        };
    }

    @Test(expected = ProvisionException.class)
    public void testCreate() throws ProvisionException {
        subject.create(null, null, null, null);
    }

    @Test
    public void testLoadWrongLocation() throws ProvisionException {
        Assert.assertNull(subject.load(URI.create("file:/testFileUri"), 0, new NullProgressMonitor()));
    }

    @Test
    public void testLoad() throws ProvisionException {
        LocalArtifactRepository repo = new LocalArtifactRepository(tempLocalMavenRepository.getLocalRepositoryIndex());
        repo.save();
        IArtifactRepository repo2 = subject.load(tempLocalMavenRepository.getLocalRepositoryRoot().toURI(), 0,
                new NullProgressMonitor());
        Assert.assertEquals(repo, repo2);
    }
}
