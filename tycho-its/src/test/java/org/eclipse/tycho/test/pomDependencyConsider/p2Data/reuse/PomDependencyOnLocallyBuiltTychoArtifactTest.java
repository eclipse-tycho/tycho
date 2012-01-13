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
import org.codehaus.plexus.DefaultPlexusContainer;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.LocalMavenRepositoryTool;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.junit.Assert;
import org.junit.Test;

public class PomDependencyOnLocallyBuiltTychoArtifactTest extends AbstractTychoIntegrationTest {

    private static final GAV TEST_PROJECT_POM_DEPENDENCY = new GAV(
            "tycho-its-project.pomDependencyConsider.p2Data.reuse",
            "pomDependencyConsider.p2Data.reuse.testDataBundle", "0.0.1");

    @Test
    public void testReuseOfP2MetadataForPomDependencyOnTychoArtifact() throws Exception {
        setUpBundleWithSourceBundle();

        // project with POM dependency on the bundle with sources
        Verifier verifier = getVerifier("pomDependencyConsider.p2Data.reuse/testProject", false);

        // this fails unless the p2-metadata.xml is reused (because the POM dependency publisher doesn't create source bundles)
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();

        String testProjectRoot = verifier.getBasedir();
        File p2RepoModule = new File(testProjectRoot, "repository");
        P2RepositoryTool p2Repo = P2RepositoryTool.forEclipseRepositoryModule(p2RepoModule);

        // check that bundle and source bundle make it into the p2 repository - this isn't obvious due to TYCHO-561
        File expectedBinaryBundle = p2Repo.getBundleArtifact(TEST_PROJECT_POM_DEPENDENCY.getArtifactId(),
                TEST_PROJECT_POM_DEPENDENCY.getVersion());
        File expectedSourceBundle = p2Repo.getBundleArtifact(TEST_PROJECT_POM_DEPENDENCY.getArtifactId() + ".source",
                TEST_PROJECT_POM_DEPENDENCY.getVersion());
        Assert.assertTrue(expectedBinaryBundle.isFile());
        Assert.assertTrue(expectedSourceBundle.isFile());
    }

    private void setUpBundleWithSourceBundle() throws Exception {
        Verifier testDataProject = getVerifier("pomDependencyConsider.p2Data.reuse/testDataBundle", false);
        testDataProject.executeGoal("install");
        testDataProject.verifyErrorFreeLog();
        // prevent that the created bundle & source bundle can be automatically seen by other Tycho builds
        LocalMavenRepositoryTool localRepo = new DefaultPlexusContainer().lookup(LocalMavenRepositoryTool.class);
        localRepo.removeLinesFromMetadataIndex(TEST_PROJECT_POM_DEPENDENCY.getGroupId() + ":"
                + TEST_PROJECT_POM_DEPENDENCY.getArtifactId() + ":" + TEST_PROJECT_POM_DEPENDENCY.getVersion());
    }
}
