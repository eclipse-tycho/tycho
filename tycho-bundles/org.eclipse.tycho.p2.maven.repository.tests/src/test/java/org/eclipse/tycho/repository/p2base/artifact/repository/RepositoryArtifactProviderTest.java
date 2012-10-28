/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.p2base.artifact.repository;

import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_A_KEY;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.NOT_CONTAINED_ARTIFACT_KEY;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.REPO_BUNDLE_A_CORRUPT;
import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderTestUtils.ANY_ARTIFACT_KEY_QUERY;
import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderTestUtils.canonicalDescriptorFor;
import static org.eclipse.tycho.test.util.StatusMatchers.errorStatus;
import static org.eclipse.tycho.test.util.StatusMatchers.statusWithMessageWhich;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.both;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.File;
import java.net.URI;
import java.util.Arrays;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderException;
import org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactTransferPolicy;
import org.eclipse.tycho.repository.p2base.artifact.provider.CompositeArtifactProviderTestBase;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.RemoteArtifactTransferPolicy;
import org.junit.Test;

@SuppressWarnings("restriction")
public class RepositoryArtifactProviderTest extends CompositeArtifactProviderTestBase<IRawArtifactProvider> {

    private static final ArtifactTransferPolicy TRANSFER_POLICY = new RemoteArtifactTransferPolicy();

    @Override
    public IRawArtifactProvider createCompositeArtifactProvider(URI... repositoryURLs) throws Exception {
        return new RepositoryArtifactProvider(Arrays.asList(repositoryURLs), TRANSFER_POLICY, p2Context.getAgent());
    }

    @Test(expected = ArtifactProviderException.class)
    public void testRepositoryLoadingFails() throws Exception {
        URI locationWithoutArtifactRepository = new File(".").getAbsoluteFile().toURI().normalize();
        subject = createCompositeArtifactProvider(locationWithoutArtifactRepository);

        subject.query(ANY_ARTIFACT_KEY_QUERY, null);
    }

    @Test
    public void testGetNonExistingArtifact() {
        IStatus result = subject.getArtifact(NOT_CONTAINED_ARTIFACT_KEY, testSink, null);

        assertThat(result, is(errorStatus()));
        assertThat(result,
                statusWithMessageWhich(containsString("is not available in any of the following repositories: ")));
    }

    @Test
    public void testGetNonExistingRawArtifact() {
        IStatus result = subject.getRawArtifact(canonicalDescriptorFor(NOT_CONTAINED_ARTIFACT_KEY), testSink, null);

        assertThat(result, is(errorStatus()));
        assertThat(result,
                statusWithMessageWhich(containsString("is not available in any of the following repositories: ")));
    }

    @Test
    public void testErrorWhileGettingArtifact() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNDLE_A_CORRUPT);

        IStatus result = subject.getArtifact(BUNDLE_A_KEY, testSink, null);

        assertThat(result, is(errorStatus()));
        assertThat(result, statusWithMessageWhich(both(containsString("An error occurred while transferring artifact"))
                .and(containsString(REPO_BUNDLE_A_CORRUPT.toString()))));
    }

    @Test
    public void testErrorWhileGettingRawArtifact() throws Exception {
        subject = createCompositeArtifactProvider(REPO_BUNDLE_A_CORRUPT);

        IStatus result = subject.getRawArtifact(canonicalDescriptorFor(BUNDLE_A_KEY), testSink, null);

        assertThat(result, is(errorStatus()));
        assertThat(result, statusWithMessageWhich(both(containsString("An error occurred while transferring artifact"))
                .and(containsString(REPO_BUNDLE_A_CORRUPT.toString()))));
    }

}
