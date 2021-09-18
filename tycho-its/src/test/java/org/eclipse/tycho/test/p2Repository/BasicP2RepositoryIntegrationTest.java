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

import static org.eclipse.tycho.test.util.TychoMatchers.isFile;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.nio.file.Files;

import org.apache.maven.it.Verifier;
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
		verifier.getCliOptions().add("-Dtest-data-repo=" + ResourceUtil.P2Repositories.ECLIPSE_LATEST.toString());
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		p2Repo = P2RepositoryTool.forEclipseRepositoryModule(new File(verifier.getBasedir()));
	}

	@Test
	public void test381377BundleInclusion() {
		// check that (separately!) included bundle is there
		assertThat(p2Repo.getBundleArtifact("osgi.enterprise", "4.2.0.v201108120515"), isFile());
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
		assertThat(repositoryArchive, isFile());
	}

	@Test
	public void testResourcesProcessed() throws Exception {
		File repository = new File(verifier.getBasedir(), "target/repository");
		assertThat(new File(repository, "index.html"), isFile());
		File aboutFile = new File(repository, "about/about.html");
		assertThat(aboutFile, isFile());
		assertThat(Files.readString(aboutFile.toPath()).trim(), equalTo("About testrepo"));
	}

	@Test
	public void testXZCompression() throws Exception {
		File repository = new File(verifier.getBasedir(), "target/repository");
		assertThat(new File(repository, "content.xml.xz"), isFile());
		assertThat(new File(repository, "artifacts.xml.xz"), isFile());
		assertThat(new File(repository, "p2.index"), isFile());
	}

}
