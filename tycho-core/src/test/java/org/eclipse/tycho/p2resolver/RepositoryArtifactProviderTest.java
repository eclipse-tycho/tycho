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
 *    Tobias Oberlies (SAP SE) - initial API and implementation
 *    Christoph Läubrich - adjust test to changed exception message
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import static org.eclipse.tycho.test.util.ArtifactRepositoryTestUtils.ANY_ARTIFACT_KEY_QUERY;
import static org.eclipse.tycho.test.util.ArtifactRepositoryTestUtils.canonicalDescriptorFor;
import static org.eclipse.tycho.test.util.ProbeArtifactSink.newArtifactSinkFor;
import static org.eclipse.tycho.test.util.ProbeRawArtifactSink.newRawArtifactSinkFor;
import static org.eclipse.tycho.test.util.StatusMatchers.errorStatus;
import static org.eclipse.tycho.test.util.StatusMatchers.warningStatus;
import static org.eclipse.tycho.test.util.TestRepositoryContent.BUNDLE_A_KEY;
import static org.eclipse.tycho.test.util.TestRepositoryContent.BUNDLE_B_KEY;
import static org.eclipse.tycho.test.util.TestRepositoryContent.REPO_BUNDLE_A_CORRUPT;
import static org.eclipse.tycho.test.util.TestRepositoryContent.REPO_BUNLDE_AB_PACK_CORRUPT;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.tycho.IRawArtifactProvider;
import org.eclipse.tycho.p2.repository.AbstractArtifactRepository2;
import org.eclipse.tycho.p2.repository.ArtifactTransferPolicies;
import org.eclipse.tycho.p2.repository.ArtifactTransferPolicy;
import org.eclipse.tycho.p2.repository.RepositoryArtifactProvider;
import org.junit.Test;

public class RepositoryArtifactProviderTest extends CompositeArtifactProviderTestBase<IRawArtifactProvider> {

    private static final ArtifactTransferPolicy TRANSFER_POLICY = ArtifactTransferPolicies.forLocalArtifacts();

    @Override
    protected IRawArtifactProvider createCompositeArtifactProvider(URI... repositoryURLs) throws Exception {
        return new RepositoryArtifactProvider(Arrays.asList(repositoryURLs), TRANSFER_POLICY,
                lookup(IProvisioningAgent.class));
    }

    @Test
    public void testRepositoryLoadingFails() throws Exception {

        URI locationWithoutArtifactRepository = new File("nonRepoLocation").getAbsoluteFile().toURI();
        subject = createCompositeArtifactProvider(locationWithoutArtifactRepository);

        Exception e = assertThrows(Exception.class, () -> subject.query(ANY_ARTIFACT_KEY_QUERY, null));
        assertThat(e.getMessage(),
                both(containsString("Load repository from url")).and(containsString("nonRepoLocation")));
    }

    @Test
    public void testGetArtifactErrorMessage() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNDLE_A_CORRUPT);

        testSink = newArtifactSinkFor(BUNDLE_A_KEY);
        status = subject.getArtifact(testSink, null);

        assertThat(status, is(errorStatus()));
        String string = REPO_BUNDLE_A_CORRUPT.toString();
        if (string.endsWith("/")) {
            string = string.substring(0, string.length() - 1);
        }
        assertThat(status.getMessage(),
                both(containsString("An error occurred while transferring artifact")).and(containsString(string)));
        assertFalse(testSink.writeIsCommitted());
    }

    @Test
    public void testGetRawArtifactErrorMessage() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNDLE_A_CORRUPT);

        rawTestSink = newRawArtifactSinkFor(canonicalDescriptorFor(BUNDLE_A_KEY));
        status = subject.getRawArtifact(rawTestSink, null);

        assertThat(status, is(errorStatus()));
        String string = REPO_BUNDLE_A_CORRUPT.toString();
        if (string.endsWith("/")) {
            string = string.substring(0, string.length() - 1);
        }
        assertThat(status.getMessage(),
                both(containsString("An error occurred while transferring artifact")).and(containsString(string)));
        assertFalse(rawTestSink.writeIsCommitted());
    }

    @Test
    public void testGetArtifactWhereOnlyFormatIsCorrupt() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNLDE_AB_PACK_CORRUPT);

        testSink = newArtifactSinkFor(BUNDLE_B_KEY);
        status = subject.getArtifact(testSink, null);

        assertThat(status, is(errorStatus()));
        assertFalse(testSink.writeIsCommitted());
    }

    @Test
    public void testGetArtifactWithSomeMirrorFailures() throws Exception {
        IArtifactRepository failingMirrorsRepository = createArtifactRepositoryMock();
        when(failingMirrorsRepository.contains(BUNDLE_A_KEY)).thenReturn(true);
        when(failingMirrorsRepository.getArtifactDescriptors(BUNDLE_A_KEY))
                .thenReturn(new IArtifactDescriptor[] { canonicalDescriptorFor(BUNDLE_A_KEY) });
        when(failingMirrorsRepository.getArtifact(eq(canonicalDescriptorFor(BUNDLE_A_KEY)), any(OutputStream.class),
                any(IProgressMonitor.class))).thenReturn(errorWithRetry("mirror 1 failure")) //
                        .thenReturn(errorWithRetry("mirror 2 failure")) //
                        .thenReturn(Status.OK_STATUS);
        subject = new RepositoryArtifactProvider(Collections.singletonList(failingMirrorsRepository), TRANSFER_POLICY);

        testSink = newArtifactSinkFor(BUNDLE_A_KEY);
        status = subject.getArtifact(testSink, null);

        assertThat(status, is(warningStatus()));
        assertThat(status.toString(), containsString("mirror 1"));
        assertThat(status.toString(), containsString("mirror 2"));
    }

    @Test
    public void testGetArtifactWithInfiniteMirrorFailures() throws Exception {
        IArtifactRepository failingMirrorsRepository = createArtifactRepositoryMock();
        when(failingMirrorsRepository.contains(BUNDLE_A_KEY)).thenReturn(true);
        when(failingMirrorsRepository.getArtifactDescriptors(BUNDLE_A_KEY))
                .thenReturn(new IArtifactDescriptor[] { canonicalDescriptorFor(BUNDLE_A_KEY) });
        when(failingMirrorsRepository.getArtifact(eq(canonicalDescriptorFor(BUNDLE_A_KEY)), any(OutputStream.class),
                any(IProgressMonitor.class))).thenReturn(errorWithRetry("mirror failure"));
        subject = new RepositoryArtifactProvider(Collections.singletonList(failingMirrorsRepository), TRANSFER_POLICY);

        testSink = newArtifactSinkFor(BUNDLE_A_KEY);
        status = subject.getArtifact(testSink, null); // should give up if all mirrors fail

        assertThat(status, is(errorStatus()));
    }

    private static IArtifactRepository createArtifactRepositoryMock() {
        /*
         * Create an IArtifactRepository mock instance with the default implementation of
         * org.eclipse.equinox.p2.repository.artifact.IArtifactRepository.getArtifacts(
         * IArtifactRequest[], IProgressMonitor)
         */
        IArtifactRepository partialMock = mock(AbstractArtifactRepository2.class);
        when(partialMock.getArtifacts(any(IArtifactRequest[].class), any(IProgressMonitor.class))).thenCallRealMethod();
        return partialMock;
    }

    private static IStatus errorWithRetry(String message) {
        return new Status(IStatus.ERROR, "test", IArtifactRepository.CODE_RETRY, "Stub error: " + message, null);
    }

}
