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

import java.io.File;
import java.net.URI;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.LocalArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.LocalArtifactRepositoryFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LocalArtifactRepositoryFactoryTest {

    private LocalArtifactRepositoryFactory subject;

    private final File basedir = new File("target/repository/"
            + LocalArtifactRepositoryFactoryTest.class.getSimpleName()).getAbsoluteFile();

    @Before
    public void cleanupRepository() {
        deleteDir(basedir);
    }

    private void deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDir(file);

                }
                file.delete();
            }
        }
    }

    @Before
    public void setUp() {
        subject = new LocalArtifactRepositoryFactory();
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
        LocalArtifactRepository repo = new LocalArtifactRepository(basedir);
        repo.save();
        IArtifactRepository repo2 = subject.load(basedir.toURI(), 0, new NullProgressMonitor());
        Assert.assertEquals(repo, repo2);
    }
}
