/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

public class P2ArtifactMappingToMavenRepoTest extends AbstractTychoIntegrationTest {

	@Test
	public void testMapperReferenceMavenCentral() throws Exception {
		Verifier verifier = getVerifier("p2Repository.mavenRepo");
		verifier.addCliArgument("-Dtest-data-repo=" + ResourceUtil.P2Repositories.ECLIPSE_352.toString());
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
		File repository = new File(verifier.getBasedir(), "target/repository");
		File artifactsXML = new File(repository, "artifacts.xml");
		assertTrue(
				artifactsXML.getAbsoluteFile() + " does not contain required line "
						+ RepositorySystem.DEFAULT_REMOTE_REPO_URL,
				Files.readAllLines(artifactsXML.toPath()).stream()
						.anyMatch(line -> line.contains(RepositorySystem.DEFAULT_REMOTE_REPO_URL)));
	}
}
