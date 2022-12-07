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

import java.io.File;
import java.util.Properties;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.HttpServer;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.eclipse.tycho.test.util.TargetDefinitionUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PasswordProtectedP2RepositoryTest extends AbstractTychoIntegrationTest {

	private HttpServer server;
	private String p2RepoUrl;

	@Before
	public void startServer() throws Exception {
		server = HttpServer.startServer("test-user", "test-password");
		p2RepoUrl = server.addServer("foo", ResourceUtil.resolveTestResource("repositories/e342"));
	}

	@After
	public void stopServer() throws Exception {
		server.stop();
	}

	@Test
	public void testRepository() throws Exception {
		Verifier verifier = createVerifier("settings.xml");
		verifier.addCliArgument("-P=repository");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testRepositoryEncrypted() throws Exception {
		Verifier verifier = createVerifier("settings-encrypted.xml", "settings-security.xml");
		verifier.addCliArgument("-P=repository");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testTargetDefinition() throws Exception {
		Verifier verifier = createVerifier("settings.xml");
		File platformFile = new File(verifier.getBasedir(), "platform.target");
		TargetDefinitionUtil.setRepositoryURLs(platformFile, p2RepoUrl);
		verifier.addCliArgument("-P=target-definition");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testTargetDefinitionEncrypted() throws Exception {
		Verifier verifier = createVerifier("settings-encrypted.xml", "settings-security.xml");
		File platformFile = new File(verifier.getBasedir(), "platform.target");
		TargetDefinitionUtil.setRepositoryURLs(platformFile, p2RepoUrl);
		verifier.addCliArgument("-P=target-definition");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

	private Verifier createVerifier(String settingsFile) throws Exception {
		return createVerifier(settingsFile, null);
	}

	private Verifier createVerifier(String settingsFile, String settingsSecurityFile) throws Exception {
		Verifier verifier = getVerifier("target.httpAuthentication", false,
				new File("projects/target.httpAuthentication/" + settingsFile));
		Properties systemProperties = verifier.getSystemProperties();
		systemProperties.setProperty("p2.repo", p2RepoUrl);
		if (settingsSecurityFile != null) {
			// see
			// org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher#SYSTEM_PROPERTY_SEC_LOCATION
			systemProperties.setProperty("settings.security",
					new File("projects/target.httpAuthentication/" + settingsSecurityFile).getAbsolutePath());
		}
		return verifier;
	}
}
