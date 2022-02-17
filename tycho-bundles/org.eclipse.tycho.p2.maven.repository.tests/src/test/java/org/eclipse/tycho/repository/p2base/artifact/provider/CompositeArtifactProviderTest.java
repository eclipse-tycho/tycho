/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP SE and others.
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
package org.eclipse.tycho.repository.p2base.artifact.provider;

import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_A_KEY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.net.URI;
import java.util.Collections;

import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicies;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicy;
import org.eclipse.tycho.repository.p2base.artifact.repository.FileRepositoryArtifactProvider;
import org.junit.Test;

public class CompositeArtifactProviderTest extends CompositeArtifactProviderTestBase<IRawArtifactFileProvider> {

    private static final ArtifactTransferPolicy TRANSFER_POLICY = ArtifactTransferPolicies.forLocalArtifacts();

    @Override
    protected IRawArtifactFileProvider createCompositeArtifactProvider(URI... repositoryURLs) throws Exception {
        // load repositories as separate providers, and join them with the provider under test
        return new CompositeArtifactProvider(toRawArtifactFileProviders(repositoryURLs));
    }

    private IRawArtifactFileProvider[] toRawArtifactFileProviders(URI... repositoryURLs) throws ProvisionException {
        IRawArtifactFileProvider[] components = new IRawArtifactFileProvider[repositoryURLs.length];
        for (int ix = 0; ix < repositoryURLs.length; ix++) {
            components[ix] = new FileRepositoryArtifactProvider(Collections.singletonList(repositoryURLs[ix]),
                    TRANSFER_POLICY, p2Context.getAgent());
        }
        return components;
    }

    @Test
    public void testGetArtifactFile() {
        File result = subject.getArtifactFile(BUNDLE_A_KEY);

        assertThat(result, is(artifactInLocalRepo(BUNDLE_A_KEY, TestRepositoryContent.REPO_BUNDLE_A, ".jar")));
    }

    private static File artifactInLocalRepo(IArtifactKey key, URI localRepository, String extension) {
        return new File(new File(localRepository), "plugins/" + key.getId() + "_" + key.getVersion() + extension);
    }
}
