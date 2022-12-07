/*******************************************************************************
 * Copyright (c) 2008, 2018 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.target;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.HttpServer;
import org.eclipse.tycho.test.util.TargetDefinitionUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OfflineModeTest extends AbstractTychoIntegrationTest {

	private HttpServer server;

	@Before
	public void startServer() throws Exception {
		server = HttpServer.startServer();
	}

	@After
	public void stopServer() throws Exception {
		server.stop();
	}

	@Test
	public void testWithSimpleRepository() throws Exception {
		Verifier verifier = getVerifierAndSetupServerAndRepo("target.offlineMode", "repo");
		runAndVerifyOnlineBuild(verifier);
		runAndVerifyOfflineBuild(verifier);
	}

	@Test
	public void testWithXZRepository() throws Exception {
		Verifier verifier = getVerifierAndSetupServerAndRepo("target.offlineModeXZRepo", "repo");
		runAndVerifyOnlineBuild(verifier);
		runAndVerifyOfflineBuild(verifier);
	}

	@Test
	public void testWithCompositeRepository() throws Exception {
		Verifier verifier = getVerifierAndSetupServerAndRepo("target.offlineModeCompositeRepo", "compositeRepo");
		server.addServer("test/childOne", new File(verifier.getBasedir(), "compositeRepo/child1"));
		server.addServer("test/childTwo", new File(verifier.getBasedir(), "compositeRepo/child2"));

		runAndVerifyOnlineBuild(verifier);
		runAndVerifyOfflineBuild(verifier);

	}

	private Verifier getVerifierAndSetupServerAndRepo(String basedir, String repoName) throws Exception, IOException {
		Verifier verifier = getVerifier(basedir, false);
		String url = server.addServer("test", new File(verifier.getBasedir(), repoName));
		verifier.addCliArgument("-Dp2.repo=" + url);

		File platformFile = new File(verifier.getBasedir(), "platform.target");
		TargetDefinitionUtil.setRepositoryURLs(platformFile, url);
		return verifier;
	}

	private void runAndVerifyOfflineBuild(Verifier verifier) throws VerificationException {
		verifier.addCliArgument("--offline");
		verifier.setLogFileName("log-offline.txt");
		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();
		Set<String> urls = new LinkedHashSet<>(server.getAccessedUrls("test"));
		assertTrue(urls.toString(), urls.isEmpty());
	}

	private void runAndVerifyOnlineBuild(Verifier verifier) throws VerificationException {
		verifier.setLogFileName("log-online.txt");
		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();
		assertFalse(server.getAccessedUrls("test").isEmpty());
		server.getAccessedUrls("test").clear();
	}

}
