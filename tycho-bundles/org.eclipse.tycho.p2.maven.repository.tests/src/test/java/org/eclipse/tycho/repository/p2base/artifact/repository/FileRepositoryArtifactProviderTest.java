/*******************************************************************************
 * Copyright (c) 2012, 2020 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - adjust to new API
 *******************************************************************************/
package org.eclipse.tycho.repository.p2base.artifact.repository;

import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_A_KEY;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.REPO_BUNDLE_A;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.REPO_BUNDLE_AB;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicies;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicy;
import org.eclipse.tycho.test.util.P2Context;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class FileRepositoryArtifactProviderTest {

    private static final ArtifactTransferPolicy TRANSFER_POLICY = ArtifactTransferPolicies.forLocalArtifacts();

    @Rule
    public P2Context p2Context = new P2Context();

    private IRawArtifactFileProvider subject;

    @Before
    public void initContextAndSubject() throws Exception {
        subject = new FileRepositoryArtifactProvider(Arrays.asList(REPO_BUNDLE_A, REPO_BUNDLE_AB), TRANSFER_POLICY,
                p2Context.getAgent());
    }

    @Test
    public void testGetArtifactFile() {
        File result = subject.getArtifactFile(BUNDLE_A_KEY);

        assertThat(result, is(artifactInLocalRepo(BUNDLE_A_KEY, TestRepositoryContent.REPO_BUNDLE_A, ".jar")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructionWithNonArtifactFileRepository() throws Exception {
        IArtifactRepository repository = mock(IArtifactRepository.class); // i.e. not an IFileArtifactRepository

        subject = new FileRepositoryArtifactProvider(loaderFor(repository), TRANSFER_POLICY);
        subject.getArtifactFile(BUNDLE_A_KEY);
    }

    private static File artifactInLocalRepo(IArtifactKey key, URI localRepository, String extension) {
        return new File(new File(localRepository), "plugins/" + key.getId() + "_" + key.getVersion() + extension);
    }

    private static ArtifactRepositorySupplier loaderFor(final IArtifactRepository repository) throws Exception {
        return () -> Collections.singletonList(repository);
    }

}
