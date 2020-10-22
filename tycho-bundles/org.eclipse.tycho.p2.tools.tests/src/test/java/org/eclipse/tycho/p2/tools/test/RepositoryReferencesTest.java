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
package org.eclipse.tycho.p2.tools.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.repository.registry.facade.RepositoryBlackboardKey;
import org.junit.Before;
import org.junit.Test;

public class RepositoryReferencesTest {
    private static final File LOCATION_A = new File("a/location");

    private static final File LOCATION_B = new File("another/location");

    private static final URI LOCATION_C = URI.create("http://example.org/some/remote/location");

    private static final RepositoryBlackboardKey LOCATION_D = RepositoryBlackboardKey
            .forResolutionContextArtifacts(new File("."));

    RepositoryReferences subject;

    @Before
    public void initSubject() {
        subject = new RepositoryReferences();
    }

    @Test
    public void testNoMetadataRepo() {
        assertEquals(0, subject.getMetadataRepositories().size());
    }

    @Test
    public void testNoArtifactRepo() {
        assertEquals(0, subject.getArtifactRepositories().size());
    }

    @Test
    public void testMetadataReposWithOrder() {
        subject.addMetadataRepository(LOCATION_B);
        subject.addMetadataRepository(LOCATION_A);
        subject.addMetadataRepository(LOCATION_C);

        List<URI> repositories = subject.getMetadataRepositories();

        assertEquals(3, repositories.size());
        assertEquals(LOCATION_B.toURI(), repositories.get(0));
        assertEquals(LOCATION_A.toURI(), repositories.get(1));
        assertEquals(LOCATION_C, repositories.get(2));
    }

    @Test
    public void testArtifactReposWithOrder() {
        subject.addArtifactRepository(LOCATION_D);
        subject.addArtifactRepository(LOCATION_B);
        subject.addArtifactRepository(LOCATION_C);
        subject.addArtifactRepository(LOCATION_A);

        List<URI> repositories = subject.getArtifactRepositories();

        assertEquals(4, repositories.size());
        assertEquals(LOCATION_D.toURI(), repositories.get(0));
        assertEquals(LOCATION_B.toURI(), repositories.get(1));
        assertEquals(LOCATION_C, repositories.get(2));
        assertEquals(LOCATION_A.toURI(), repositories.get(3));
    }

}
