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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.P2RepositoryTool.IU;
import org.junit.BeforeClass;
import org.junit.Test;

public class IUMetadataGenerationTest extends AbstractTychoIntegrationTest {

	private static P2RepositoryTool repo;

	@BeforeClass
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
		assertThat(finalIU.getProvidedCapabilities(), hasItem("org.eclipse.equinox.p2.iu/iua.artifact/1.0.0"));
		List<String> properties = finalIU.getProperties();
		assertThat(properties, hasItem("maven-groupId=" + "tycho-its-project.iu.artifact"));
		assertThat(properties, hasItem("maven-artifactId=" + "iua.artifact"));
		assertThat(properties, hasItem("maven-version=" + finalIU.getVersion()));
		assertThat(finalIU.getArtifacts(), hasItem("binary/iua.artifact/1.0.0"));

		// check that the artifact is here
		assertTrue(repo.getBinaryArtifact("iua.artifact", "1.0.0").isFile());
	}

	@Test
	public void testIUWithoutArtifact() throws Exception {
		IU finalIU = repo.getUniqueIU("iua.noartifact");

		assertThat(finalIU.getProvidedCapabilities(), hasItem("org.eclipse.equinox.p2.iu/iua.noartifact/1.0.0"));
		assertTrue(finalIU.getArtifacts().isEmpty());
		assertFalse(repo.getBinaryArtifact("iua.noartifact", "1.0.0").isFile());
	}

}
