/*******************************************************************************
 * Copyright (c) 2022 Mat Booth and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.extras.mirror;

import static java.util.Arrays.asList;
import static org.eclipse.tycho.test.util.TychoMatchers.isFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.extras.its.AbstractTychoExtrasIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.P2RepositoryTool.IU;
import org.junit.Assert;
import org.junit.Test;

public class TestMirrorRepositories extends AbstractTychoExtrasIntegrationTest {

    /*
     * The test projects are configured to mirror mockito and its deps. Mockito is interesting to
     * test because it is published with GPG signatures, so we can check that those are mirrored
     * correctly too.
     */
    private static final String MOCKITO_ID = "org.mockito.mockito-core";

    /**
     * Mirror from explicit source repository specified here - bundles are pulled directly from the
     * repository URL given in the plug-in configuration
     */
    @Test
    public void testMirroringFromExplicitSource() throws Exception {
        verifyMirrorRepo("mirrortest-from-explicit-source");
    }

    /**
     * Test mirroring from the target platform - bundles are resolved from the target platform,
     * which are resolved from the cache in ~/.m2/repository/p2
     */
    @Test
    public void testMirroringFromTargetPlatform() throws Exception {
        verifyMirrorRepo("mirrortest-from-target-platform");
    }

    private void verifyMirrorRepo(String testCase) throws Exception {
        Verifier verifier = getVerifier(testCase, false);
        File mirrorLocation = new File(verifier.getBasedir(), "target/repository");
        verifier.addCliOption("-Dmirror.location=" + mirrorLocation);
        verifier.executeGoals(asList("clean", "verify"));
        verifier.verifyErrorFreeLog();
        Assert.assertTrue("Repo mirror does not exist at " + mirrorLocation, mirrorLocation.exists());
        P2RepositoryTool repo = P2RepositoryTool.forEclipseRepositoryModule(new File(verifier.getBasedir()));

        // Ensure the bundle exists in the metadata repo and a physical artifact exists
        IU mockito = repo.getUniqueIU(MOCKITO_ID);
        assertThat(repo.getBundleArtifact(MOCKITO_ID, mockito.getVersion()), isFile());
        repo.getBundleArtifact(MOCKITO_ID, mockito.getVersion());

        // Check for mirrored GPG properties in the artifact repo -- we know these are present in the upstream repo that is being mirrored
        List<String> properties = mockito.getArtifactProperties();
        String sigBlock = null;
        String pubKeyBlock = null;
        for (String property : properties) {
            if (property.startsWith("pgp.signatures=-----BEGIN PGP SIGNATURE-----")
                    && property.endsWith("-----END PGP SIGNATURE-----")) {
                sigBlock = property;
            }
            if (property.startsWith("pgp.publicKeys=-----BEGIN PGP PUBLIC KEY BLOCK-----")
                    && property.endsWith("-----END PGP PUBLIC KEY BLOCK-----")) {
                pubKeyBlock = property;
            }
        }
        assertNotNull("No GPG signature property found in artifact repo for " + MOCKITO_ID, sigBlock);
        assertNotNull("No GPG public key property found in artifact repo for " + MOCKITO_ID, pubKeyBlock);
    }
}
