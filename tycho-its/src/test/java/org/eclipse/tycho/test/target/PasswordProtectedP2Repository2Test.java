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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.HttpServer;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.eclipse.tycho.test.util.TargetDefinitionUtil;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class PasswordProtectedP2Repository2Test extends AbstractTychoIntegrationTest {

	private static HttpServer server;
	private static String p2RepoUrl;

	@AfterClass
	public static void stopServer() throws Exception {
		server.stop();
	}

	@Parameters
	public static Collection<String> supportedURLBasedServerIds() throws Exception {
		server = HttpServer.startServer("test-user", "test-password");
		p2RepoUrl = server.addServer("foo", ResourceUtil.resolveTestResource("repositories/e342"));

		URI serverURI = URI.create(p2RepoUrl);
		String authority = serverURI.getAuthority();
		List<String> pathSegments = Arrays.stream(serverURI.getPath().split("/")).filter(s -> !s.isEmpty()).toList();
		List<String> paths = IntStream.rangeClosed(0, pathSegments.size())
				.mapToObj(i -> String.join("/", pathSegments.subList(0, i))).map(p -> p.isEmpty() ? "" : ("/" + p))
				.toList();
		return Stream.of(serverURI.getScheme() + "://", "")
				.flatMap(scheme -> paths.stream().map(p -> scheme + authority + p)).toList();
	}

	@Parameter
	public String serverId;

	@Test
	public void testTargetDefinitionWithoutLocationID() throws Exception {
		Verifier verifier = createVerifier("settings-repository-id.xml");

		File platformFile = new File(verifier.getBasedir(), "platform-without-repository-id.target");
		TargetDefinitionUtil.setRepositoryURLs(platformFile, p2RepoUrl);
		Path settingsFile = Path.of(verifier.getBasedir(), "settings-repository-id.xml");
		Files.writeString(settingsFile, Files.readString(settingsFile).replace("[serverId]", serverId));
		verifier.getCliOptions().removeIf(o -> o.startsWith("-s ") || o.startsWith("--settings"));
		verifier.addCliOption("--settings " + settingsFile);

		verifier.addCliOption("-P=target-definition-without-repository-id");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

	private Verifier createVerifier(String settingsFile) throws Exception {
		return createVerifier(settingsFile, null);
	}

	private Verifier createVerifier(String settingsFile, String settingsSecurityFile) throws Exception {
		Verifier verifier = getVerifier("target.httpAuthentication", false,
				new File("projects/target.httpAuthentication/" + settingsFile));
		verifier.setSystemProperty("p2.repo", p2RepoUrl);
		if (settingsSecurityFile != null) {
			// see
			// org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher#SYSTEM_PROPERTY_SEC_LOCATION
			verifier.setSystemProperty("settings.security",
					new File("projects/target.httpAuthentication/" + settingsSecurityFile).getAbsolutePath());
		}
		return verifier;
	}

}
