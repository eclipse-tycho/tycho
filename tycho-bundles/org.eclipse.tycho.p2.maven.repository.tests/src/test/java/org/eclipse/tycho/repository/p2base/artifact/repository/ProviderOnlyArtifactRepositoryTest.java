/*******************************************************************************
 * Copyright (c) 2013 SAP SE and others.
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
package org.eclipse.tycho.repository.p2base.artifact.repository;

import static java.util.Arrays.asList;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_A_CONTENT_MD5;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_A_FILES;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_A_KEY;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.REPO_BUNDLE_A;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.REPO_BUNDLE_AB;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.REPO_BUNLDE_AB_PACK_CORRUPT;
import static org.eclipse.tycho.repository.testutil.ArtifactRepositoryTestUtils.canonicalDescriptorFor;
import static org.eclipse.tycho.test.util.StatusMatchers.okStatus;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.URI;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicies;
import org.eclipse.tycho.repository.streaming.testutil.ProbeOutputStream;
import org.eclipse.tycho.test.util.P2Context;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

public class ProviderOnlyArtifactRepositoryTest {

    // test (legacy) methods of IArtifactRepository that are not in IRawArtifactProvider

    @ClassRule
    public static P2Context p2Context = new P2Context();

    ProbeOutputStream testOutputStream = new ProbeOutputStream();

    private ProviderOnlyArtifactRepository subject;

    @After
    public void checkStreamNotClosed() {
        // none of the tested methods should close the stream
        assertThat(testOutputStream.isClosed(), is(false));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetArtifactViaStream() throws Exception {
        subject = createProviderOnlyArtifactRepositoryDelegatingTo(REPO_BUNDLE_A);

        IStatus status = subject.getArtifact(canonicalDescriptorFor(BUNDLE_A_KEY), testOutputStream, null);

        assertThat(status, is(okStatus()));
        assertThat(testOutputStream.getFilesInZip(), is(BUNDLE_A_FILES));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetRawArtifactViaStream() throws Exception {
        subject = createProviderOnlyArtifactRepositoryDelegatingTo(TestRepositoryContent.REPO_BUNDLE_AB);

        IStatus status = subject.getRawArtifact(canonicalDescriptorFor(BUNDLE_A_KEY), testOutputStream, null);

        assertThat(status, is(okStatus()));
        assertThat(testOutputStream.md5AsHex(), is(BUNDLE_A_CONTENT_MD5));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetRawArtifactViaStreamFailsOnFirstReadAttempt() throws Exception {
        subject = createProviderOnlyArtifactRepositoryDelegatingTo(REPO_BUNLDE_AB_PACK_CORRUPT, REPO_BUNDLE_AB);

        IStatus status = subject.getRawArtifact(canonicalDescriptorFor(BUNDLE_A_KEY), testOutputStream, null);

        assertThat(testOutputStream.getStatus(), is(okStatus()));
        assertThat(status, is(okStatus()));
    }

    private static ProviderOnlyArtifactRepository createProviderOnlyArtifactRepositoryDelegatingTo(
            URI... delegatedContent) throws Exception {
        IProvisioningAgent p2Agent = p2Context.getAgent();
        IRawArtifactFileProvider artifactProvider = new FileRepositoryArtifactProvider(asList(delegatedContent),
                ArtifactTransferPolicies.forLocalArtifacts(), p2Agent);

        ProviderOnlyArtifactRepository result = new ProviderOnlyArtifactRepository(artifactProvider, p2Agent,
                URI.create("memory:test"));
        return result;
    }
}
