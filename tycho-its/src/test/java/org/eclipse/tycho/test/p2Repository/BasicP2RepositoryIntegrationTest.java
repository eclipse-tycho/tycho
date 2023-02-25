/*******************************************************************************
 * Copyright (c) 2012, 2021 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Rapicorp, Inc. - support <iu> syntax in category.xml (371983)
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.P2RepositoryTool.IU;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.BeforeClass;
import org.junit.Test;

public class BasicP2RepositoryIntegrationTest extends AbstractTychoIntegrationTest {

	private static final String CUSTOM_FINAL_NAME = "testrepo-myqualifier";

	private static Verifier verifier;
	private static P2RepositoryTool p2Repo;

	@BeforeClass
	public static void executeBuild() throws Exception {
		verifier = new BasicP2RepositoryIntegrationTest().getVerifier("/p2Repository", false);
		verifier.addCliArgument("-Dtest-data-repo=" + ResourceUtil.P2Repositories.ECLIPSE_LATEST.toString());
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		p2Repo = P2RepositoryTool.forEclipseRepositoryModule(new File(verifier.getBasedir()));
	}

	@Test
	public void test381377BundleInclusion() {
		// check that (separately!) included bundle is there
		assertTrue(p2Repo.getBundleArtifact("com.sun.el", "2.2.0.v201303151357").isFile());
	}

	@Test
	public void testIncludedIUById() throws Exception {
		assertThat(p2Repo.getAllUnitIds(), hasItem("org.apache.felix.gogo.runtime"));

		IU categoryIU = p2Repo.getUniqueIU("Test Category");
		assertThat(categoryIU.getRequiredIds(), hasItem("org.apache.felix.gogo.runtime"));
	}

	@Test
	public void testIncludeIUViaMatchQuery() throws Exception {
		assertThat(p2Repo.getAllUnitIds(), hasItem("javax.annotation"));

		IU categoryIU = p2Repo.getUniqueIU("Test Category");
		assertThat(categoryIU.getRequiredIds(), hasItem("javax.annotation"));
	}

	@Test
	public void test347416CustomFinalName() throws Exception {
		File repositoryArchive = new File(verifier.getBasedir(), "target/" + CUSTOM_FINAL_NAME + ".zip");
		assertTrue(repositoryArchive.isFile());
	}

	@Test
	public void testResourcesProcessed() throws Exception {
		File repository = new File(verifier.getBasedir(), "target/repository");
		assertTrue(new File(repository, "index.html").isFile());
		File aboutFile = new File(repository, "about/about.html");
		assertTrue(aboutFile.isFile());
		assertThat(Files.readString(aboutFile.toPath()).trim(), equalTo("About testrepo"));
	}

	@Test
	public void testXZCompression() throws Exception {
		File repository = new File(verifier.getBasedir(), "target/repository");
		assertTrue(new File(repository, "content.xml.xz").isFile());
		assertTrue(new File(repository, "artifacts.xml.xz").isFile());
		assertTrue(new File(repository, "p2.index").isFile());
	}

	@Test
	public void testDependencyList() throws Exception {
		Verifier dependencyListVerifier = getVerifier("/p2Repository.basic");
		dependencyListVerifier.addCliArgument("-Dtest-data-repo=" + ResourceUtil.P2Repositories.ECLIPSE_352);
		dependencyListVerifier.executeGoal("dependency:list");
		dependencyListVerifier.verifyErrorFreeLog();
		File logFile = new File(dependencyListVerifier.getBasedir(), dependencyListVerifier.getLogFileName());
		assertTrue(Files.lines(logFile.toPath()).anyMatch(line -> line
				.contains("p2.eclipse.plugin:org.eclipse.osgi:eclipse-plugin:3.5.2.R35x_v20100126:system")));
	}
}
