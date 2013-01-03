/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.TYCHO359corruptedArtifactDownloads;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.LocalMavenRepositoryTool;
import org.eclipse.tycho.test.util.TargetDefinitionUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * This test uses the checked-in repository at <code>repositories/e342_corrupt</code>, which was
 * originally a copy of <code>repositories/e342</code> but with all actual artifacts (i.e. jars)
 * deleted. The repository metadata ([content|artifacts].xml) was not changed.
 * 
 * Therefore the repository is corrupt to induce the intended error behavior.
 */
@SuppressWarnings("unchecked")
public class RecoveryAfterCorruptArtifactDownloadsTest extends AbstractTychoIntegrationTest {

    private static final String TESTED_BUNDLE_NAME = "tycho359.test.bundle";

    private static final String TESTED_BUNDLE_VERSION = "1.0.0";

    private static final GAV TESTED_BUNDLE_GAV = new GAV("p2.osgi.bundle", TESTED_BUNDLE_NAME, TESTED_BUNDLE_VERSION);

    private LocalMavenRepositoryTool localMavenRepositoryTool;

    @Before
    public void removeTestArtifactFromLocalRepository() throws Exception {
        localMavenRepositoryTool = new DefaultPlexusContainer().lookup(LocalMavenRepositoryTool.class);
        localMavenRepositoryTool.removeLinesFromArtifactsIndex(TESTED_BUNDLE_GAV.toExternalForm());
    }

    @Test
    public void testSuccessfulRebuildAfterCorruptedArtifactDownload() throws Exception {

        Verifier verifier = getVerifier("/TYCHO359corruptedArtifactDownloads", false);
        FileUtils.deleteDirectory(new File(new File(verifier.localRepo, "p2/osgi/bundle"), TESTED_BUNDLE_NAME));

        File targetFile = new File(verifier.getBasedir(), "invalidRepo.target");
        TargetDefinitionUtil.makeURLsAbsolute(targetFile, targetFile.getParentFile());

        // test execution
        try {
            verifier.getCliOptions().add("-Pinvalid-target-definition");
            verifier.executeGoal("package");
            Assert.fail("Build is expected to fail, but does not!");
        } catch (VerificationException e) {
            // build is failing, ignore exception
        }
        // check that tycho tries to download the intentionally unavailable artifact
        verifier.verifyTextInLog("Downloading " + TESTED_BUNDLE_NAME);

        // check that the corrupt jar file is not in the local repo p2 index
        assertFalse(localMavenRepositoryTool.getArtifactIndexLines().contains(TESTED_BUNDLE_GAV.toExternalForm()));

        Verifier verifier2 = getVerifier("/TYCHO359corruptedArtifactDownloads", false);

        File targetFile2 = new File(verifier2.getBasedir(), "validRepo.target");
        TargetDefinitionUtil.makeURLsAbsolute(targetFile2, targetFile.getParentFile());

        // test 2 execution
        verifier2.getCliOptions().add("-Pvalid-target-definition");
        verifier2.executeGoal("package");

        verifier2.verifyErrorFreeLog();

        // self-test: now the same artifact is expected to be in the index
        assertTrue(localMavenRepositoryTool.getArtifactIndexLines().contains(TESTED_BUNDLE_GAV.toExternalForm()));
    }
}
