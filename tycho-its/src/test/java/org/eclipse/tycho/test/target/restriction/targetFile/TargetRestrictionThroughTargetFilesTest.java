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
package org.eclipse.tycho.test.target.restriction.targetFile;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.TargetDefinitionUtil;
import org.junit.Test;

public class TargetRestrictionThroughTargetFilesTest extends AbstractTychoIntegrationTest {

    private Verifier verifier;

    @Test
    public void testVersionRestrictionWithPlanner() throws Exception {
        verifier = getVerifier("target.restriction.targetFile/testProject", false);
        TargetDefinitionUtil.makeURLsAbsolute(new File(getTargetsProject(), "planner.target"),
                TargetDefinitionUtil.BaseLocation.TARGET_FILE_IN_SOURCES);

        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();

        P2RepositoryTool p2Repo = P2RepositoryTool.forEclipseRepositoryModule(getRepositoryProject());
        assertTrue(p2Repo.getBundleArtifact("trt.bundle", "1.0.0.201108051343").isFile());

        // in the planner mode, optionally required things are included in the target platform 
        assertTrue(p2Repo.getBundleArtifact("trt.bundle.optional", "1.0.0.201108051328").isFile());

        // there is a newer version in the p2 repository, but only the 1.0 version is in the target platform
        assertFalse(p2Repo.getBundleArtifact("trt.bundle.referenced", "2.0.0.201108051319").isFile());
        assertTrue(p2Repo.getBundleArtifact("trt.bundle.referenced", "1.0.0.201108051343").isFile());

        assertTrue(p2Repo.findFeatureArtifact("trt.feature").isFile());
    }

    @Test
    public void testContentAndVersionRestrictionWithSlicer() throws Exception {
        verifier = getVerifier("target.restriction.targetFile/testProject", false);
        verifier.getCliOptions().add("-Pwith-slicer-target");
        TargetDefinitionUtil.makeURLsAbsolute(new File(getTargetsProject(), "slicer.target"),
                TargetDefinitionUtil.BaseLocation.TARGET_FILE_IN_SOURCES);

        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();

        P2RepositoryTool p2Repo = P2RepositoryTool.forEclipseRepositoryModule(getRepositoryProject());
        assertTrue(p2Repo.getBundleArtifact("trt.bundle", "1.0.0.201108051343").isFile());

        // in the slicer mode, only included content is part of the target platform
        assertFalse(p2Repo.getBundleArtifact("trt.bundle.optional", "1.0.0.201108051328").isFile());

        // there is a newer version in the p2 repository, but only the 1.0 version is in the target platform
        assertFalse(p2Repo.getBundleArtifact("trt.bundle.referenced", "2.0.0.201108051319").isFile());
        assertTrue(p2Repo.getBundleArtifact("trt.bundle.referenced", "1.0.0.201108051343").isFile());

        assertTrue(p2Repo.findFeatureArtifact("trt.feature").isFile());
    }

    private File getTargetsProject() {
        return new File(verifier.getBasedir(), "trt.targets");
    }

    private File getRepositoryProject() {
        return new File(verifier.getBasedir(), "trt.assembly");
    }
}
