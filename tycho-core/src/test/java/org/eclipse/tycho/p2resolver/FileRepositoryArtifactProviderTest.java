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
package org.eclipse.tycho.p2resolver;

import static org.eclipse.tycho.test.util.TestRepositoryContent.BUNDLE_A_KEY;
import static org.eclipse.tycho.test.util.TestRepositoryContent.REPO_BUNDLE_AB;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.tycho.p2.artifact.provider.IRawArtifactFileProvider;
import org.eclipse.tycho.p2.repository.ArtifactRepositorySupplier;
import org.eclipse.tycho.p2.repository.ArtifactTransferPolicies;
import org.eclipse.tycho.p2.repository.ArtifactTransferPolicy;
import org.eclipse.tycho.p2.repository.FileRepositoryArtifactProvider;
import org.eclipse.tycho.test.util.TestRepositoryContent;
import org.eclipse.tycho.testing.TychoPlexusTestCase;
import org.junit.Before;
import org.junit.Test;

public class FileRepositoryArtifactProviderTest extends TychoPlexusTestCase {

    private static final ArtifactTransferPolicy TRANSFER_POLICY = ArtifactTransferPolicies.forLocalArtifacts();

    private IRawArtifactFileProvider subject;

    @Before
    public void initContextAndSubject() throws Exception {
        subject = new FileRepositoryArtifactProvider(
                Arrays.asList(TestRepositoryContent.REPO2_BUNDLE_A, REPO_BUNDLE_AB), TRANSFER_POLICY,
                lookup(IProvisioningAgent.class));
    }

    @Test
    public void testGetArtifactFile() {
        File result = subject.getArtifactFile(BUNDLE_A_KEY);

        assertEquals(artifactInLocalRepo(BUNDLE_A_KEY, TestRepositoryContent.REPO2_BUNDLE_A, ".jar"), result);
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
