/*******************************************************************************
 * Copyright (c) 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Patrick Ziegler - initial API and implementation
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

public class PasswordProtectedM2RepositoryTest extends AbstractTychoIntegrationTest {

	private HttpServer server;
	private String m2RepoUrl;

	@Before
	public void startServer() throws Exception {
		server = HttpServer.startServer("test-user", "test-password");
		m2RepoUrl = server.addServer("foo", ResourceUtil.resolveTestResource("repositories/m2"));
	}

	@After
	public void stopServer() throws Exception {
		server.stop();
	}

	@Test
	public void testTargetDefinition() throws Exception {
		Verifier verifier = createVerifier("settings.xml");
		File platformBundle = new File(verifier.getBasedir(), "target.test");
		File platformFile = new File(platformBundle, "platform.target");
		TargetDefinitionUtil.setRepositoryURLs(platformFile, m2RepoUrl);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

	private Verifier createVerifier(String settingsFile) throws Exception {
		Verifier verifier = getVerifier("target.maven.httpAuthentication", false,
				new File("projects/target.maven.httpAuthentication/" + settingsFile));
		Properties systemProperties = verifier.getSystemProperties();
		systemProperties.setProperty("m2.repo", m2RepoUrl);
		return verifier;
	}
}
