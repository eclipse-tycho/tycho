/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.p2base.artifact.repository;

import static java.util.Arrays.asList;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_A_KEY;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_B_KEY;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.REPO_BUNDLE_A_CORRUPT;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.REPO_BUNLDE_AB_PACK_CORRUPT;
import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderTestUtils.ANY_ARTIFACT_KEY_QUERY;
import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderTestUtils.canonicalDescriptorFor;
import static org.eclipse.tycho.repository.p2base.artifact.provider.ProbeArtifactSink.newArtifactSinkFor;
import static org.eclipse.tycho.repository.p2base.artifact.provider.ProbeRawArtifactSink.newRawArtifactSinkFor;
import static org.eclipse.tycho.test.util.StatusMatchers.errorStatus;
import static org.eclipse.tycho.test.util.StatusMatchers.warningStatus;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.URI;
import java.util.Arrays;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent;
import org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactTransferPolicy;
import org.eclipse.tycho.repository.p2base.artifact.provider.CompositeArtifactProviderTestBase;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.RemoteArtifactTransferPolicy;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RepositoryArtifactProviderTest extends CompositeArtifactProviderTestBase<IRawArtifactProvider> {

    private static final ArtifactTransferPolicy TRANSFER_POLICY = new RemoteArtifactTransferPolicy();

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
        IStatus result = subject.getArtifact(testSink, null);

        assertThat(result, is(errorStatus()));
        assertThat(
                result.getMessage(),
                both(containsString("An error occurred while transferring artifact")).and(
                        containsString(REPO_BUNDLE_A_CORRUPT.toString())));
        assertThat(testSink.writeIsCommitted(), is(false));
    }

    @Test
    public void testGetRawArtifactErrorMessage() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNDLE_A_CORRUPT);

        rawTestSink = newRawArtifactSinkFor(canonicalDescriptorFor(BUNDLE_A_KEY));
        IStatus result = subject.getRawArtifact(rawTestSink, null);

        assertThat(result, is(errorStatus()));
        assertThat(
                result.getMessage(),
                both(containsString("An error occurred while transferring artifact")).and(
                        containsString(REPO_BUNDLE_A_CORRUPT.toString())));
        assertThat(rawTestSink.writeIsCommitted(), is(false));
    }

    @Test
    public void testGetArtifactWherePreferredFormatIsCorrupt() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNLDE_AB_PACK_CORRUPT);

        testSink = newArtifactSinkFor(BUNDLE_A_KEY);
        IStatus status = subject.getArtifact(testSink, null);

        assertThat(status, is(warningStatus()));
        assertThat(asList(status.getChildren()), hasItem(errorStatus()));
        assertThat(testSink.getFilesInZip(), is(TestRepositoryContent.BUNDLE_A_FILES));
    }

    // TODO change test to AllFormatsAreCorrupt?
    @Test
    public void testGetArtifactWhereOnlyFormatIsCorrupt() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNLDE_AB_PACK_CORRUPT);

        testSink = newArtifactSinkFor(BUNDLE_B_KEY);
        IStatus status = subject.getArtifact(testSink, null);

        assertThat(status, is(errorStatus()));
        assertThat(testSink.writeIsCommitted(), is(false));
    }
}
