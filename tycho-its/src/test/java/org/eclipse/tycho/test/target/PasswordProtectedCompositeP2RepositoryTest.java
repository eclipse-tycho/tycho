/*******************************************************************************
 * Copyright (c) 23 Arm Limited and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Arm Limited - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.target;

import java.io.File;
import java.util.Properties;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.HttpServer;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.eclipse.tycho.test.util.TargetDefinitionUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PasswordProtectedCompositeP2RepositoryTest extends AbstractTychoIntegrationTest {

	private HttpServer server;
	private String p2RepoUrl;

	private HttpServer mirror;
	private String p2MirrorUrl;

	private HttpServer authMirror;
	private String p2AuthMirrorUrl;

	@Before
	public void startServer() throws Exception {
		server = HttpServer.startServer("test-user", "test-password");
		p2RepoUrl = server.addServer("foo", ResourceUtil.resolveTestResource("repositories/issue_2331_reproducer"))
				+ "/bundles";

		mirror = HttpServer.startServer();
		p2MirrorUrl = mirror.addServer("bar", ResourceUtil.resolveTestResource("repositories/issue_2331_reproducer"))
				+ "/bundles";

		authMirror = HttpServer.startServer("mirror-user", "mirror-password");
		p2AuthMirrorUrl = authMirror.addServer("bar",
				ResourceUtil.resolveTestResource("repositories/issue_2331_reproducer")) + "/bundles";
	}

	@After
	public void stopServer() throws Exception {
		authMirror.stop();
		mirror.stop();
		server.stop();
	}

	/**
	 * Tries to access a composite p2 repository over an authenticated composite
	 * mirror.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAuthMirror() throws Exception {
		Verifier verifier = createVerifier("settings-auth-mirror.xml");
		verifier.setSystemProperty("p2.authMirror", p2AuthMirrorUrl);
		verifier.addCliOption("-P=repository");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

	/**
	 * Tries to access a composite p2 repository over a composite mirror with no
	 * authentication.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMirror() throws Exception {
		Verifier verifier = createVerifier("settings-mirror.xml");
		verifier.setSystemProperty("p2.mirror", p2MirrorUrl);
		verifier.addCliOption("-P=repository");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

	/**
	 * Tries to access a composite p2 repository.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRepository() throws Exception {
		Verifier verifier = createVerifier("settings.xml");
		verifier.addCliOption("-P=repository");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

	/**
	 * Tries to access an authenticated composite p2 repository whose password is
	 * encrypted.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRepositoryEncrypted() throws Exception {
		Verifier verifier = createVerifier("settings-encrypted.xml", "settings-security.xml");
		verifier.addCliOption("-P=repository");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

	/**
	 * Tries to resolve a target platform from a composite p2 repository.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTargetDefinition() throws Exception {
		Verifier verifier = createVerifier("settings.xml");
		File platformFile = new File(verifier.getBasedir(), "bundle/platform.target");
		TargetDefinitionUtil.setRepositoryURLs(platformFile, p2RepoUrl);
		verifier.addCliOption("-P=target-definition");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

	/**
	 * Tries to resolve a target definition from a composite p2 repository accessed
	 * over a composite mirror with no authentication.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTargetDefinitionMirror() throws Exception {
		Verifier verifier = createVerifier("settings-mirror.xml");
		File platformFile = new File(verifier.getBasedir(), "bundle/platform.target");
		TargetDefinitionUtil.setRepositoryURLs(platformFile, p2RepoUrl);
		verifier.setSystemProperty("p2.mirror", p2MirrorUrl);
		verifier.addCliOption("-P=target-definition");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

	/**
	 * Tries to resolve a target definition from a composite p2 repository accessed
	 * over an authenticated composite mirror.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTargetDefinitionAuthMirror() throws Exception {
		Verifier verifier = createVerifier("settings-auth-mirror.xml");
		File platformFile = new File(verifier.getBasedir(), "bundle/platform.target");
		TargetDefinitionUtil.setRepositoryURLs(platformFile, p2RepoUrl);
		verifier.setSystemProperty("p2.authMirror", p2AuthMirrorUrl);
		verifier.addCliOption("-P=target-definition");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

	/**
	 * Tries to resolve a target definition from an authenticated composite p2
	 * repository whose password is encrypted.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTargetDefinitionEncrypted() throws Exception {
		Verifier verifier = createVerifier("settings-encrypted.xml", "settings-security.xml");
		File platformFile = new File(verifier.getBasedir(), "bundle/platform.target");
		TargetDefinitionUtil.setRepositoryURLs(platformFile, p2RepoUrl);
		verifier.addCliOption("-P=target-definition");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

	private Verifier createVerifier(String settingsFile) throws Exception {
		return createVerifier(settingsFile, null);
	}

	private Verifier createVerifier(String settingsFile, String settingsSecurityFile) throws Exception {
		Verifier verifier = getVerifier("issue2331", false, new File("projects/issue2331/" + settingsFile));
		Properties systemProperties = verifier.getSystemProperties();
		systemProperties.setProperty("p2.repo", p2RepoUrl);
		if (settingsSecurityFile != null) {
			// see
			// org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher#SYSTEM_PROPERTY_SEC_LOCATION
			systemProperties.setProperty("settings.security",
					new File("projects/issue2331/" + settingsSecurityFile).getAbsolutePath());
		}
		return verifier;
	}

}
