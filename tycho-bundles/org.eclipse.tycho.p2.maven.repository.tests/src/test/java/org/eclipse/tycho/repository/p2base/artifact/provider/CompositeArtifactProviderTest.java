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
package org.eclipse.tycho.repository.p2base.artifact.provider;

import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_A_KEY;
import static org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent.BUNDLE_B_KEY;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.util.Collections;

import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.p2.maven.repository.tests.TestRepositoryContent;
import org.eclipse.tycho.repository.p2base.artifact.repository.FileRepositoryArtifactProvider;
import org.junit.Test;

public class CompositeArtifactProviderTest extends CompositeArtifactProviderTestBase<IRawArtifactFileProvider> {

    private static final ArtifactTransferPolicy TRANSFER_POLICY = new LocalArtifactTransferPolicy();

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

    @Test
    public void testGetRawArtifactFile() {
        IArtifactDescriptor packedBundleB = subject.getArtifactDescriptors(BUNDLE_B_KEY)[0];
        assertTrue(ArtifactTransferPolicy.isPack200Format(packedBundleB));

        File result = subject.getArtifactFile(packedBundleB);

        assertThat(result, is(artifactInLocalRepo(BUNDLE_B_KEY, TestRepositoryContent.REPO_BUNDLE_AB, ".jar.pack.gz")));
    }

    private static File artifactInLocalRepo(IArtifactKey key, URI localRepository, String extension) {
        return new File(new File(localRepository), "plugins/" + key.getId() + "_" + key.getVersion() + extension);
    }
}
