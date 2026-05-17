/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.target;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.TargetDefinitionUtil;
import org.junit.Test;

public class TargetRestrictionThroughTargetFilesTest extends AbstractTychoIntegrationTest {

	private Verifier verifier;

	@Test
	public void testWithFile() throws Exception {
		verifier = getVerifier("target.usefile", false);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testWithFileAbsolute() throws Exception {
		verifier = getVerifier("target.usefileAbsolute", false);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testVersionRestrictionWithPlanner() throws Exception {
		verifier = getVerifier("target.restriction.targetFile/testProject", false);
		TargetDefinitionUtil.makeURLsAbsolute(new File(getTargetsProject(), "planner.target"),
				new File("projects/target.restriction.targetFile/testProject/trt.targets"));

		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();

		P2RepositoryTool p2Repo = P2RepositoryTool.forEclipseRepositoryModule(getRepositoryProject());
		assertTrue(p2Repo.getBundleArtifact("trt.bundle", "1.0.0.201108051343").isFile());

		// in the planner mode, optionally required things are included in the target
		// platform
		assertTrue(p2Repo.getBundleArtifact("trt.bundle.optional", "1.0.0.201108051328").isFile());

		// there is a newer version in the p2 repository, but only the 1.0 version is in
		// the target platform
		assertFalse(p2Repo.getBundleArtifact("trt.bundle.referenced", "2.0.0.201108051319").isFile());
		assertTrue(p2Repo.getBundleArtifact("trt.bundle.referenced", "1.0.0.201108051343").isFile());

		assertTrue(p2Repo.findFeatureArtifact("trt.feature").isPresent());
	}

	@Test
	public void testContentAndVersionRestrictionWithSlicer() throws Exception {
		verifier = getVerifier("target.restriction.targetFile/testProject", false);
		verifier.addCliArgument("-Pwith-slicer-target");
		TargetDefinitionUtil.makeURLsAbsolute(new File(getTargetsProject(), "slicer.target"),
				new File("projects/target.restriction.targetFile/testProject/trt.targets"));

		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();

		P2RepositoryTool p2Repo = P2RepositoryTool.forEclipseRepositoryModule(getRepositoryProject());
		assertTrue(p2Repo.getBundleArtifact("trt.bundle", "1.0.0.201108051343").isFile());

		// in the slicer mode, only included content is part of the target platform
		assertFalse(p2Repo.getBundleArtifact("trt.bundle.optional", "1.0.0.201108051328").isFile());

		// there is a newer version in the p2 repository, but only the 1.0 version is in
		// the target platform
		assertFalse(p2Repo.getBundleArtifact("trt.bundle.referenced", "2.0.0.201108051319").isFile());
		assertTrue(p2Repo.getBundleArtifact("trt.bundle.referenced", "1.0.0.201108051343").isFile());

		assertTrue(p2Repo.findFeatureArtifact("trt.feature").isPresent());
	}

	private File getTargetsProject() {
		return new File(verifier.getBasedir(), "trt.targets");
	}

	private File getRepositoryProject() {
		return new File(verifier.getBasedir(), "trt.assembly");
	}
}
