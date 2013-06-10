/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Oberlies (SAP AG) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.p2base.artifact.repository;

import static java.util.Arrays.asList;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_A_KEY;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_B_KEY;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.REPO_BUNDLE_A_CORRUPT;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.REPO_BUNLDE_AB_PACK_CORRUPT;
import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderTestUtils.ANY_ARTIFACT_KEY_QUERY;
import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderTestUtils.canonicalDescriptorFor;
import static org.eclipse.tycho.repository.streaming.testutil.ProbeArtifactSink.newArtifactSinkFor;
import static org.eclipse.tycho.repository.streaming.testutil.ProbeRawArtifactSink.newRawArtifactSinkFor;
import static org.eclipse.tycho.test.util.StatusMatchers.errorStatus;
import static org.eclipse.tycho.test.util.StatusMatchers.warningStatus;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent;
import org.eclipse.tycho.repository.p2base.artifact.provider.CompositeArtifactProviderTestBase;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicies;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicy;
import org.eclipse.tycho.repository.p2base.artifact.repository.RepositoryArtifactProvider.RepositoryLoader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RepositoryArtifactProviderTest extends CompositeArtifactProviderTestBase<IRawArtifactProvider> {

    private static final ArtifactTransferPolicy TRANSFER_POLICY = ArtifactTransferPolicies.forRemoteArtifacts();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    protected IRawArtifactProvider createCompositeArtifactProvider(URI... repositoryURLs) throws Exception {
        return new RepositoryArtifactProvider(Arrays.asList(repositoryURLs), TRANSFER_POLICY, p2Context.getAgent());
    }

    @Test
    public void testRepositoryLoadingFails() throws Exception {
        expectedException.expectMessage(both(containsString("No repository found")).and(
                containsString("nonRepoLocation")));

        URI locationWithoutArtifactRepository = new File("nonRepoLocation").getAbsoluteFile().toURI();
        subject = createCompositeArtifactProvider(locationWithoutArtifactRepository);

        subject.query(ANY_ARTIFACT_KEY_QUERY, null);
    }

    @Test
    public void testGetArtifactErrorMessage() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNDLE_A_CORRUPT);

        testSink = newArtifactSinkFor(BUNDLE_A_KEY);
        status = subject.getArtifact(testSink, null);

        assertThat(status, is(errorStatus()));
        assertThat(
                status.getMessage(),
                both(containsString("An error occurred while transferring artifact")).and(
                        containsString(REPO_BUNDLE_A_CORRUPT.toString())));
        assertThat(testSink.writeIsCommitted(), is(false));
    }

    @Test
    public void testGetRawArtifactErrorMessage() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNDLE_A_CORRUPT);

        rawTestSink = newRawArtifactSinkFor(canonicalDescriptorFor(BUNDLE_A_KEY));
        status = subject.getRawArtifact(rawTestSink, null);

        assertThat(status, is(errorStatus()));
        assertThat(
                status.getMessage(),
                both(containsString("An error occurred while transferring artifact")).and(
                        containsString(REPO_BUNDLE_A_CORRUPT.toString())));
        assertThat(rawTestSink.writeIsCommitted(), is(false));
    }

    @Test
    public void testGetArtifactWherePreferredFormatIsCorrupt() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNLDE_AB_PACK_CORRUPT);

        testSink = newArtifactSinkFor(BUNDLE_A_KEY);
        status = subject.getArtifact(testSink, null);

        assertThat(status, is(warningStatus()));
        assertThat(asList(status.getChildren()), hasItem(errorStatus()));
        assertThat(testSink.getFilesInZip(), is(TestRepositoryContent.BUNDLE_A_FILES));
    }

    @Test
    public void testGetArtifactWhereOnlyFormatIsCorrupt() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNLDE_AB_PACK_CORRUPT);

        testSink = newArtifactSinkFor(BUNDLE_B_KEY);
        status = subject.getArtifact(testSink, null);

        assertThat(status, is(errorStatus()));
        assertThat(testSink.writeIsCommitted(), is(false));
    }

    @Test
    public void testGetArtifactWithSomeMirrorFailures() throws Exception {
        IArtifactRepository failingMirrorsRepository = mock(IArtifactRepository.class);
        when(failingMirrorsRepository.contains(BUNDLE_A_KEY)).thenReturn(true);
        when(failingMirrorsRepository.getArtifactDescriptors(BUNDLE_A_KEY)).thenReturn(
                new IArtifactDescriptor[] { canonicalDescriptorFor(BUNDLE_A_KEY) });
        when(
                failingMirrorsRepository.getArtifact(argThat(is(canonicalDescriptorFor(BUNDLE_A_KEY))),
                        any(OutputStream.class), any(IProgressMonitor.class)))
                .thenReturn(errorWithRetry("mirror 1 failure")) //
                .thenReturn(errorWithRetry("mirror 2 failure")) //
                .thenReturn(Status.OK_STATUS);
        subject = new RepositoryArtifactProvider(loaderFor(failingMirrorsRepository), TRANSFER_POLICY);

        testSink = newArtifactSinkFor(BUNDLE_A_KEY);
        status = subject.getArtifact(testSink, null);

        assertThat(status, is(warningStatus()));
        assertThat(status.toString(), containsString("mirror 1"));
        assertThat(status.toString(), containsString("mirror 2"));
    }

    @Test
    public void testGetArtifactWithInfiniteMirrorFailures() throws Exception {
        IArtifactRepository failingMirrorsRepository = mock(IArtifactRepository.class);
        when(failingMirrorsRepository.contains(BUNDLE_A_KEY)).thenReturn(true);
        when(failingMirrorsRepository.getArtifactDescriptors(BUNDLE_A_KEY)).thenReturn(
                new IArtifactDescriptor[] { canonicalDescriptorFor(BUNDLE_A_KEY) });
        when(
                failingMirrorsRepository.getArtifact(argThat(is(canonicalDescriptorFor(BUNDLE_A_KEY))),
                        any(OutputStream.class), any(IProgressMonitor.class))).thenReturn(
                errorWithRetry("mirror failure"));
        subject = new RepositoryArtifactProvider(loaderFor(failingMirrorsRepository), TRANSFER_POLICY);

        testSink = newArtifactSinkFor(BUNDLE_A_KEY);
        status = subject.getArtifact(testSink, null); // should give up if all mirrors fail

        assertThat(status, is(errorStatus()));
    }

    private static IStatus errorWithRetry(String message) {
        return new Status(IStatus.ERROR, "test", IArtifactRepository.CODE_RETRY, "Stub error: " + message, null);
    }

    private RepositoryLoader loaderFor(final IArtifactRepository repository) throws Exception {
        return new RepositoryLoader(null, p2Context.getAgent()) {
            @Override
            List<IArtifactRepository> loadRepositories() {
                return Collections.singletonList(repository);
            }
        };
    }

}
