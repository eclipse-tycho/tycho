/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.TYCHO359corruptedArtifactDownloads;

import java.io.File;
import java.io.IOException;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.tycho.model.Target;
import org.eclipse.tycho.model.Target.Repository;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
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

    @Test
    public void testSuccessfulRebuildAfterCorruptedArtifactDownload() throws Exception {

        Verifier verifier = getVerifier("/TYCHO359corruptedArtifactDownloads", false);
        FileUtils.deleteDirectory(new File(new File(verifier.localRepo, "p2/osgi/bundle"), TESTED_BUNDLE_NAME));

        File targetFile = new File(verifier.getBasedir(), "invalidRepo.target");
        convertLocationPathsToAbsoluteURIs(targetFile);

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

        // check that the corrupt jar file is not in the local repo
        verifier.assertArtifactNotPresent("p2.osgi.bundle", TESTED_BUNDLE_NAME, TESTED_BUNDLE_VERSION, "jar");

        Verifier verifier2 = getVerifier("/TYCHO359corruptedArtifactDownloads", false);

        File targetFile2 = new File(verifier2.getBasedir(), "validRepo.target");
        convertLocationPathsToAbsoluteURIs(targetFile2);

        // test 2 execution
        verifier2.getCliOptions().add("-Pvalid-target-definition");
        verifier2.executeGoal("package");

        verifier2.verifyErrorFreeLog();
    }

    /**
     * Reads the given targetFile and converts all paths in locations into absolute file URIs.
     * 
     * Paths are expected to be relative to tycho-its directory.
     * 
     * TODO move this to a util class at org.eclipse.tycho.test.util and reuse in other tests.
     * 
     * @param targetFile
     *            the target definition file to be processed
     */
    private void convertLocationPathsToAbsoluteURIs(File targetFile) throws IOException, XmlPullParserException {
        Target platform = Target.read(targetFile);

        for (Target.Location location : platform.getLocations()) {
            for (Repository repository : location.getRepositories()) {
                File file = new File(repository.getLocation());
                repository.setLocation(file.getCanonicalFile().toURI().toASCIIString());
            }
        }

        Target.write(platform, targetFile);
    }
}
