/*******************************************************************************
 * Copyright (c) 2013, 2022 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tobias Oberlies (SAP SE) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import static java.util.Arrays.asList;
import static org.eclipse.tycho.test.util.ArtifactRepositoryTestUtils.canonicalDescriptorFor;
import static org.eclipse.tycho.test.util.TestRepositoryContent.BUNDLE_A_CONTENT_MD5;
import static org.eclipse.tycho.test.util.TestRepositoryContent.BUNDLE_A_FILES;
import static org.eclipse.tycho.test.util.TestRepositoryContent.BUNDLE_A_KEY;
import static org.eclipse.tycho.test.util.TestRepositoryContent.REPO_BUNDLE_AB;
import static org.eclipse.tycho.test.util.TestRepositoryContent.REPO_BUNLDE_AB_PACK_CORRUPT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.tycho.IRawArtifactFileProvider;
import org.eclipse.tycho.p2.repository.ArtifactTransferPolicies;
import org.eclipse.tycho.p2.repository.FileRepositoryArtifactProvider;
import org.eclipse.tycho.p2.repository.ProviderOnlyArtifactRepository;
import org.eclipse.tycho.test.util.ProbeOutputStream;
import org.eclipse.tycho.test.util.TestRepositoryContent;
import org.eclipse.tycho.testing.TychoPlexusTestCase;
import org.junit.After;
import org.junit.Test;

public class ProviderOnlyArtifactRepositoryTest extends TychoPlexusTestCase {

    // test (legacy) methods of IArtifactRepository that are not in IRawArtifactProvider

    ProbeOutputStream testOutputStream = new ProbeOutputStream();

    private ProviderOnlyArtifactRepository subject;

    @After
    public void checkStreamNotClosed() {
        // none of the tested methods should close the stream
        assertFalse(testOutputStream.isClosed());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetArtifactViaStream() throws Exception {
        subject = createProviderOnlyArtifactRepositoryDelegatingTo(TestRepositoryContent.REPO2_BUNDLE_A);

        IStatus status = subject.getArtifact(canonicalDescriptorFor(BUNDLE_A_KEY), testOutputStream, null);

        assertTrue(status.isOK());
        assertEquals(BUNDLE_A_FILES, testOutputStream.getFilesInZip());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetRawArtifactViaStream() throws Exception {
        subject = createProviderOnlyArtifactRepositoryDelegatingTo(TestRepositoryContent.REPO_BUNDLE_AB);

        IStatus status = subject.getRawArtifact(canonicalDescriptorFor(BUNDLE_A_KEY), testOutputStream, null);

        assertTrue(status.isOK());
        assertEquals(BUNDLE_A_CONTENT_MD5, testOutputStream.md5AsHex());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetRawArtifactViaStreamFailsOnFirstReadAttempt() throws Exception {
        subject = createProviderOnlyArtifactRepositoryDelegatingTo(REPO_BUNLDE_AB_PACK_CORRUPT, REPO_BUNDLE_AB);

        IStatus status = subject.getRawArtifact(canonicalDescriptorFor(BUNDLE_A_KEY), testOutputStream, null);

        assertTrue(testOutputStream.getStatus().isOK());
        assertTrue(status.isOK());
    }

    private ProviderOnlyArtifactRepository createProviderOnlyArtifactRepositoryDelegatingTo(URI... delegatedContent)
            throws Exception {
        IProvisioningAgent p2Agent = lookup(IProvisioningAgent.class);
        IRawArtifactFileProvider artifactProvider = new FileRepositoryArtifactProvider(asList(delegatedContent),
                ArtifactTransferPolicies.forLocalArtifacts(), p2Agent);

        ProviderOnlyArtifactRepository result = new ProviderOnlyArtifactRepository(artifactProvider, p2Agent,
                URI.create("memory:test"));
        return result;
    }
}
