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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PasswordProtectedCompositeP2RepositoryTest extends AbstractTychoIntegrationTest {

	private HttpServer server;
	private String p2RepoUrl;

	@Before
	public void startServer() throws Exception {
		server = HttpServer.startServer("test-user", "test-password");
		p2RepoUrl = server.addServer("foo", ResourceUtil.resolveTestResource("repositories/issue_2331_reproducer"))
				+ "/bundles";
	}

	@After
	public void stopServer() throws Exception {
		server.stop();
	}

	@Test
	public void testRepository() throws Exception {
		Verifier verifier = createVerifier("settings.xml");
		verifier.addCliOption("-P=repository");
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
					new File("projects/target.httpAuthentication/" + settingsSecurityFile).getAbsolutePath());
		}
		return verifier;
	}

}
