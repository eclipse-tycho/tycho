/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.pomDependencyConsider.p2Data.reuse;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.LocalMavenRepositoryTool;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

public class PomDependencyOnRemoteTychoArtifactTest extends AbstractTychoIntegrationTest {
    private static final GAV ITS_PROJECT_POM_DEPENDENCY = new GAV("org.sonatype.tycho", "org.sonatype.tycho.p2",
            "0.11.0");

    /**
     * In this test we want to force the implementation to trigger the download of the
     * p2-artifacts.xml/p2-metadata.xml of the POM dependency artifact into the local Maven
     * repository. Therefore we check that this has not been done before.
     * 
     * This precondition should be met sufficiently frequently (e.g. in a CI build that starts with
     * an empty local Maven repository), so no (potentially fragile) actions are taken to enforce
     * the precondition. Instead, the test is just skipped if the precondition is not met.
     */
    @BeforeClass
    public static void assumeNoLocalP2FilesForPomDepencency() throws Exception {
        Assume.assumeTrue(!getP2ArtifactsXmlInLocalRepo().exists());
        Assume.assumeTrue(!getP2MetadataXmlInLocalRepo().exists());
    }

    @Test
    public void testThatP2DataIsDownloadedForPomDependency() throws Exception {
        // this project declares a POM depencency on a bundle built by Tycho
        Verifier verifier = getVerifier("pomDependencyConsider.p2Data.reuse.DownloadTest", false);

        verifier.setMavenDebug(true);
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();

        String testProjectRoot = verifier.getBasedir();
        File p2RepoModule = new File(testProjectRoot, "repository");
        P2RepositoryTool p2Repo = P2RepositoryTool.forEclipseRepositoryModule(p2RepoModule);

        // this was bug TYCHO-570: the build passed, but the POM dependency bundle was missing
        File expectedBundle = p2Repo.getBundleArtifact(ITS_PROJECT_POM_DEPENDENCY.getArtifactId(),
                ITS_PROJECT_POM_DEPENDENCY.getVersion());
        Assert.assertTrue(expectedBundle.canRead());

        /*
         * Check that p2-artifacts.xml/p2-metadata.xml for the POM dependency have been downloaded.
         * Due to other test in this package we can have confidence that the p2 data from these
         * files has acutually been used for the build.
         */
        Assert.assertTrue(getP2ArtifactsXmlInLocalRepo().isFile());
        Assert.assertTrue(getP2MetadataXmlInLocalRepo().isFile());
    }

    private static File getP2ArtifactsXmlInLocalRepo() {
        LocalMavenRepositoryTool localRepo = new LocalMavenRepositoryTool();
        File p2ArtifactsXml = localRepo.getArtifactFile(ITS_PROJECT_POM_DEPENDENCY,
                RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS, RepositoryLayoutHelper.EXTENSION_P2_ARTIFACTS);
        return p2ArtifactsXml;
    }

    private static File getP2MetadataXmlInLocalRepo() {
        LocalMavenRepositoryTool localRepo = new LocalMavenRepositoryTool();
        File p2MetadataXml = localRepo.getArtifactFile(ITS_PROJECT_POM_DEPENDENCY,
                RepositoryLayoutHelper.CLASSIFIER_P2_METADATA, RepositoryLayoutHelper.EXTENSION_P2_METADATA);
        return p2MetadataXml;
    }
}
