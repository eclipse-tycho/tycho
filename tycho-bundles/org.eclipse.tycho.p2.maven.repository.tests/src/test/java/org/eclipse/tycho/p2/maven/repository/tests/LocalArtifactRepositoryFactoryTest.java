/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.maven.repository.tests;

import java.net.URI;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.LocalArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.LocalArtifactRepositoryFactory;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LocalArtifactRepositoryFactoryTest extends BaseMavenRepositoryTest {

    private LocalArtifactRepositoryFactory subject;

    @Before
    public void setUp() {
        subject = new LocalArtifactRepositoryFactory() {

            @Override
            protected LocalRepositoryP2Indices lookupLocalRepoIndices() {
                return localRepoIndices;
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
        LocalArtifactRepository repo = new LocalArtifactRepository(localRepoIndices);
        repo.save();
        IArtifactRepository repo2 = subject.load(baseDir.toURI(), 0, new NullProgressMonitor());
        Assert.assertEquals(repo, repo2);
    }
}
