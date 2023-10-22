/*******************************************************************************
 * Copyright (c) 2023, 2023 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Vasili Gulevich - initial implementation
 *******************************************************************************/
package org.eclipse.tycho.test.tycho2938;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.HttpServer;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.eclipse.tycho.test.util.TargetDefinitionUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.SAXException;

public class ContentJarTest extends AbstractTychoIntegrationTest {
	private HttpServer server;
	private static final String TARGET_FEATURE_PATH = "/features/issue_2938_reproducer_1.0.0.202310211419.jar";
	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();
	private Verifier verifier;
	private String mainRepoUrl;

	@Before
	public void startServer() throws Exception {
		server = HttpServer.startServer();
		File repositoryRoot = temporaryFolder.getRoot();
		this.mainRepoUrl = server.addServer("repoA", repositoryRoot);
		File originalResource = ResourceUtil.resolveTestResource("repositories/content_jar");
		FileUtils.copyDirectory(originalResource, repositoryRoot);
		verifier = getVerifier("target.content_jar", false);
		verifier.deleteArtifacts("p2.org.eclipse.update.feature", "issue_2938_reproducer", "1.0.0.202310211419");
	}

	@After
	public void stopServer() throws Exception {
		if (server != null) {
			server.stop();
		}
	}

	@Test
	public void noRedirect() throws Exception {
		configureRepositoryInTargetDefinition(mainRepoUrl);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
		assertVisited(TARGET_FEATURE_PATH);
	}

	@Test
	public void redirectKeepFilename() throws Exception {
		String redirectedUrl = server.addRedirect("repoB", originalPath -> mainRepoUrl + originalPath);
		configureRepositoryInTargetDefinition(redirectedUrl);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
		assertVisited(TARGET_FEATURE_PATH);
	}

	@Test
	public void redirectToBadLocation() throws Exception {
		String redirectedUrl = server.addRedirect("repoB", originalPath -> mainRepoUrl + originalPath + "_invalid");
		configureRepositoryInTargetDefinition(redirectedUrl);
		Assert.assertThrows(VerificationException.class, () -> verifier.executeGoal("package"));
		verifier.verifyTextInLog("No repository found at " + redirectedUrl);
		assertVisited("/content.jar_invalid");
	}

	@Test
	public void redirectToMangledLocations() throws Exception {
		File repositoryRoot = temporaryFolder.getRoot();
		mangleFileNames(repositoryRoot.toPath());

		// https://github.com/eclipse-tycho/tycho/issues/2938
		// Redirect may change extension.
		String redirectedUrl = server.addRedirect("repoB", originalPath -> mainRepoUrl + originalPath + "_invalid");

		configureRepositoryInTargetDefinition(redirectedUrl);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
		assertVisited("/content.jar_invalid");
		assertVisited(TARGET_FEATURE_PATH + "_invalid");
	}

	private void assertVisited(String path) {
		List<String> accessedUrls = server.getAccessedUrls("repoA");
		Assert.assertTrue(String.format("Path %s should be visited, %s were visited instead", path, accessedUrls),
				accessedUrls.contains(path));
	}

	private void mangleFileNames(Path repositoryRoot) throws IOException {
		Files.walkFileTree(repositoryRoot, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.move(file, file.getParent().resolve(file.getFileName() + "_invalid"));
				return super.visitFile(file, attrs);
			}
		});
	}

	private void configureRepositoryInTargetDefinition(String url)
			throws IOException, ParserConfigurationException, SAXException {
		File platformFile = new File(verifier.getBasedir(), "targetplatform.target");
		TargetDefinitionUtil.setRepositoryURLs(platformFile, "repoA", url);
	}

}
