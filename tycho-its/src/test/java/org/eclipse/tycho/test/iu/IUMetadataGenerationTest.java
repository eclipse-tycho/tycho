/*******************************************************************************
 * Copyright (c) 2015 Rapicorp, Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Rapicorp, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.iu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.P2RepositoryTool.IU;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class IUMetadataGenerationTest extends AbstractTychoIntegrationTest {

	private static P2RepositoryTool repo;

	@BeforeAll
	public static void runBuild() throws Exception {
		Verifier verifier = new IUMetadataGenerationTest().getVerifier("iu.artifact", false);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();

		File repoProject = new File(verifier.getBasedir(), "repository");
		repo = P2RepositoryTool.forEclipseRepositoryModule(repoProject);
	}

	@Test
	public void testIUWithArtifact() throws Exception {
		IU finalIU = repo.getUniqueIU("iua.artifact");

		// Here we check that the final IU contained in the repo has the right shape
		List<String> providedCapabilities = finalIU.getProvidedCapabilities();
		assertTrue(providedCapabilities.contains("org.eclipse.equinox.p2.iu/iua.artifact/1.0.0"),
				providedCapabilities.toString());
		List<String> properties = finalIU.getProperties();
		assertTrue(properties.contains("maven-groupId=" + "tycho-its-project.iu.artifact"), properties.toString());
		assertTrue(properties.contains("maven-artifactId=" + "iua.artifact"), properties.toString());
		assertTrue(properties.contains("maven-version=" + finalIU.getVersion()), properties.toString());
		List<String> artifacts = finalIU.getArtifacts();
		assertTrue(artifacts.contains("binary/iua.artifact/1.0.0"), artifacts.toString());

		// check that the artifact is here
		assertTrue(repo.getBinaryArtifact("iua.artifact", "1.0.0").isFile());
	}

	@Test
	public void testIUWithoutArtifact() throws Exception {
		IU finalIU = repo.getUniqueIU("iua.noartifact");

		List<String> providedCapabilities = finalIU.getProvidedCapabilities();
		assertTrue(providedCapabilities.contains("org.eclipse.equinox.p2.iu/iua.noartifact/1.0.0"),
				providedCapabilities.toString());
		assertTrue(finalIU.getArtifacts().isEmpty());
		assertFalse(repo.getBinaryArtifact("iua.noartifact", "1.0.0").isFile());
	}

}
