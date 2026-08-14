/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.HttpServer;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.eclipse.tycho.test.util.TargetDefinitionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end test for the Tycho p2 transport cache. A small Tycho project
 * resolves a single bundle from a p2 repository served by an embedded Jetty
 * instance; the build is executed twice with the same Maven local repository
 * (which also hosts Tycho's p2 transport cache). After the second build the
 * bundle JAR must not have been requested again, proving that the cache is
 * actually used end to end.
 */
public class P2TransportCacheTest extends AbstractTychoIntegrationTest {

	private static final String REPO_ID = "repoA";
	private static final String BUNDLE_ID = "tycho.its.p2cache.bundle";
	private static final String ARTIFACT_PATH = "/plugins/" + BUNDLE_ID + "_1.0.0.jar";

	@TempDir
	Path tempFolder;

	private HttpServer server;

	@BeforeEach
	public void startServer() throws Exception {
		server = HttpServer.startServer();
	}

	@AfterEach
	public void stopServer() throws Exception {
		if (server != null) {
			server.stop();
		}
	}

	@Test
	public void warmCacheDoesNotRefetch() throws Exception {
		Verifier verifier = getVerifier("transport.p2cache", false);
		File repoDir = ResourceUtil.resolveTestResource("repositories/transport.p2cache");
		String repoUrl = server.addServer(REPO_ID, repoDir);

		File platformFile = new File(verifier.getBasedir(), "targetplatform.target");
		TargetDefinitionUtil.setRepositoryURLs(platformFile, REPO_ID, repoUrl);

		// Isolate Tycho's p2 transport cache from the developer's ~/.m2 so
		// previous runs cannot mask a real fetch. The same directory is reused
		// across both invocations below, so the second build must hit the cache.
		File cacheDir = Files.createDirectory(tempFolder.resolve("tycho-transport-cache")).toFile();
		verifier.setSystemProperty("tycho.p2.transport.cache", cacheDir.getAbsolutePath());
		// Tycho also surfaces resolved p2 artifacts under p2/osgi/bundle/... in
		// the Maven local repository; wipe any leftover from a previous run.
		verifier.deleteArtifacts("p2.osgi.bundle", BUNDLE_ID, "1.0.0");

		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		int firstRunFetches = Collections.frequency(server.getAccessedUrls(REPO_ID), ARTIFACT_PATH);
		assertTrue(firstRunFetches >= 1, "Bundle JAR should be fetched on cold-cache build, accessed urls were: "
				+ server.getAccessedUrls(REPO_ID));

		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		int secondRunFetches = Collections.frequency(server.getAccessedUrls(REPO_ID), ARTIFACT_PATH);
		assertEquals(firstRunFetches, secondRunFetches, "Bundle JAR was re-fetched on warm-cache build, accessed urls were: "
				+ server.getAccessedUrls(REPO_ID));
	}
}
