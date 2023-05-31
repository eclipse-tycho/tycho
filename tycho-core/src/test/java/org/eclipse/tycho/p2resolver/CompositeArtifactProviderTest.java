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
package org.eclipse.tycho.p2resolver;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URI;
import java.util.Collections;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.tycho.IRawArtifactFileProvider;
import org.eclipse.tycho.p2.repository.ArtifactTransferPolicies;
import org.eclipse.tycho.p2.repository.ArtifactTransferPolicy;
import org.eclipse.tycho.p2.repository.CompositeArtifactProvider;
import org.eclipse.tycho.p2.repository.FileRepositoryArtifactProvider;
import org.eclipse.tycho.test.util.TestRepositoryContent;
import org.junit.Test;

public class CompositeArtifactProviderTest extends CompositeArtifactProviderTestBase<IRawArtifactFileProvider> {

    private static final ArtifactTransferPolicy TRANSFER_POLICY = ArtifactTransferPolicies.forLocalArtifacts();

    @Override
    protected IRawArtifactFileProvider createCompositeArtifactProvider(URI... repositoryURLs) throws Exception {
        // load repositories as separate providers, and join them with the provider under test
        return new CompositeArtifactProvider(toRawArtifactFileProviders(repositoryURLs));
    }

    private IRawArtifactFileProvider[] toRawArtifactFileProviders(URI... repositoryURLs)
            throws ProvisionException, ComponentLookupException {
        IRawArtifactFileProvider[] components = new IRawArtifactFileProvider[repositoryURLs.length];
        for (int ix = 0; ix < repositoryURLs.length; ix++) {
            components[ix] = new FileRepositoryArtifactProvider(Collections.singletonList(repositoryURLs[ix]),
                    TRANSFER_POLICY, lookup(IProvisioningAgent.class));
        }
        return components;
    }

    @Test
    public void testGetArtifactFile() {
        File result = subject.getArtifactFile(TestRepositoryContent.BUNDLE_A_KEY);

        assertEquals(
                artifactInLocalRepo(TestRepositoryContent.BUNDLE_A_KEY, TestRepositoryContent.REPO2_BUNDLE_A, ".jar"),
                result);
    }

    private static File artifactInLocalRepo(IArtifactKey key, URI localRepository, String extension) {
        return new File(new File(localRepository), "plugins/" + key.getId() + "_" + key.getVersion() + extension);
    }
}
