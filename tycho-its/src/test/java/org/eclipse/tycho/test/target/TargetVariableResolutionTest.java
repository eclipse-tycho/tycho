/*******************************************************************************
 * Copyright (c) 2023 Vaclav Hala and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * Contributors:
 *    Vaclav Hala - initial API and implementation
 *
 *******************************************************************************/
package org.eclipse.tycho.test.target;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.HttpServer;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.P2RepositoryTool.RepositoryReference;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TargetVariableResolutionTest extends AbstractTychoIntegrationTest {
	private HttpServer server;
	private String baseurl;

	@Before
	public void startServer() throws Exception {
		server = HttpServer.startServer();
		server.addServer("repo", ResourceUtil.resolveTestResource("repositories/javax.xml"));
		var urlWithContextPath = server.getUrl("");
		baseurl = urlWithContextPath.endsWith("/") // double slash causes trouble in RepositoryTransport.download
				? urlWithContextPath.substring(0, urlWithContextPath.length() - 1)
				: urlWithContextPath;
	}

	@After
	public void stopServer() throws Exception {
		server.stop();
	}

	@Test
	public void repositoryUrlCanContainEnvVarVariable() throws Exception {
		Verifier verifier = getVerifier("target.variables/env", false);
		verifier.setEnvironmentVariable("MY_MIRROR", baseurl);
		verifier.executeGoals(Arrays.asList("package"));
		verifier.verifyErrorFreeLog();
		verifier.verifyTextInLog("validate-target-platform");
		verifyResolution(verifier);
	}

	@Test
	public void repositoryUrlCanContainSystemPropertyVariable() throws Exception {
		Verifier verifier = getVerifier("target.variables/sysprop", false);
		verifier.setSystemProperty("myMirror", baseurl);
		verifier.executeGoals(Arrays.asList("package"));
		verifier.verifyErrorFreeLog();
		verifier.verifyTextInLog("validate-target-platform");
		verifyResolution(verifier);
	}

	/**
	 * Verify that the update site has the target platform variables resolved
	 * correctly.
	 */
	private void verifyResolution(Verifier verifier) throws Exception {
		final Path sitePath = Paths.get(verifier.getBasedir(), "site");
		P2RepositoryTool p2Repo = P2RepositoryTool.forEclipseRepositoryModule(sitePath.toFile());
		List<RepositoryReference> allRepositoryReferences = p2Repo.getAllRepositoryReferences();
		// artifact + metadata
		assertEquals(2, allRepositoryReferences.size());
		final String REPO = baseurl + "/repo";
		assertThat(allRepositoryReferences,
				containsInAnyOrder(new RepositoryReference(REPO, IRepository.TYPE_ARTIFACT, IRepository.ENABLED),
						new RepositoryReference(REPO, IRepository.TYPE_METADATA, IRepository.ENABLED)));
	}
}
