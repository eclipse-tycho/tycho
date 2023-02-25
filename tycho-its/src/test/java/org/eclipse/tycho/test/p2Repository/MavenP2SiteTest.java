/*******************************************************************************
 * Copyright (c) 2021Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class MavenP2SiteTest extends AbstractTychoIntegrationTest {

	@Test
	public void testProduceConsume() throws Exception {
		{// parent
			Verifier verifier = getVerifier("p2mavensite", false);
			verifier.executeGoals(asList("install"));
			verifier.verifyErrorFreeLog();
		}
		{ // producer
			Verifier verifier = getVerifier("p2mavensite/producer", false);
			verifier.executeGoals(asList("clean", "install"));
			verifier.verifyErrorFreeLog();
			verifyRepositoryExits(verifier, "");
		}
		{ // consumer
			Verifier verifier = getVerifier("p2mavensite/consumer", false);
			verifier.executeGoals(asList("clean", "verify"));
			verifier.verifyErrorFreeLog();
		}
	}

	@Test
	public void testDeployIgnore() throws Exception {
		Verifier verifier = getVerifier("p2mavensite.reactor", false);
		verifier.executeGoals(asList("install"));
		verifier.verifyErrorFreeLog();
		verifyRepositoryExits(verifier, "site/");
		String artifacts = Files.readString(Paths.get(verifier.getBasedir(), "site/target/repository/artifacts.xml"),
				StandardCharsets.UTF_8);
		assertTrue("artifact to deploy is missing", artifacts.contains("id='org.eclipse.tycho.it.deployme'"));
		assertFalse("artifact is deployed but should't", artifacts.contains("id='org.eclipse.tycho.it.ignoreme'"));
		assertFalse("artifact is deployed but should't",
				artifacts.contains("id='org.eclipse.tycho.it.ignoreme-property'"));
		assertFalse("There should be no plugins folder!",
				new File(verifier.getBasedir(), "site/target/repository/plugins/").exists());
		assertFalse("There should be no features folder!",
				new File(verifier.getBasedir(), "site/target/repository/features/").exists());
	}

	@Test
	public void testPGP() throws Exception {
		Verifier verifier = getVerifier("p2mavensite.pgp", false);
		verifier.executeGoals(asList("clean", "install"));
		verifier.verifyErrorFreeLog();
		verifyRepositoryExits(verifier, "site/");
	}

	protected void verifyRepositoryExits(Verifier verifier, String subdir) {
		File artifacts = new File(verifier.getBasedir(), subdir + "target/repository/artifacts.xml");
		assertTrue(artifacts.getAbsolutePath() + " is missing", artifacts.exists());
		File content = new File(verifier.getBasedir(), subdir + "target/repository/content.xml");
		assertTrue(content.getAbsolutePath() + " is missing", content.exists());
		File site = new File(verifier.getBasedir(), subdir + "target/p2-site.zip");
		assertTrue(site.getAbsolutePath() + " is missing", site.exists());
	}
}
