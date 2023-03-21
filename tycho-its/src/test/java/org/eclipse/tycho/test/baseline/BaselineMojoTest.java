/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.baseline;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Before;
import org.junit.Test;

public class BaselineMojoTest extends AbstractTychoIntegrationTest {

	private static File baselineRepo = null;

	@Before
	public void buildBaselineRepository() throws Exception {
		if (baselineRepo == null) {
			File repoLocation = buildBaseRepo();
			baselineRepo = new File("target/projects", getClass().getSimpleName() + "/baselineRepo").getAbsoluteFile();
			FileUtils.copyDirectoryStructure(repoLocation, baselineRepo);
		}
	}

	/**
	 * Compares the baseline against itself...
	 *
	 * @throws Exception
	 */
	@Test
	public void testUnchangedApi() throws Exception {
		buildBaselineProject("api-bundle", false);
	}

	/**
	 * This adds a method to the interface
	 *
	 * @throws Exception
	 */
	@Test
	public void testAddMethod() throws Exception {
		// test adding a default method to the "public" interface
		Verifier verifier;
		verifier = buildBaselineProject("add-method", true);
		verifyBaselineProblem(verifier, "ADDED", "METHOD", "concat(java.lang.String,java.lang.String)", "1.0.0",
				"1.1.0");
		// test with "internal" package but extensions disabled
		verifier = buildBaselineProject("add-method-internal", true);
		verifyBaselineProblem(verifier, "ADDED", "METHOD", "newMethodButItIsInternal()", "1.0.1", "2.0.0");
		// now enable extensions, then this should pass
		verifier = buildBaselineProject("add-method-internal", false, "-Dtycho.baseline.extensions=true");
	}

	/**
	 * This adds a resource to the bundle
	 *
	 * @throws Exception
	 */
	@Test
	public void testAddResource() throws Exception {
		// if version is not bumped this should fail
		Verifier verifier = buildBaselineProject("add-resource", true);
		verifyBaselineProblem(verifier, "ADDED", "RESOURCE", "NewFile.txt", "1.0.0", "1.0.1");
		// but if we bump the version even the smallest amout it must pass
		buildBaselineProject("add-resource-with-bump", false);
	}

	/**
	 * This adds a resource to the bundle
	 *
	 * @throws Exception
	 */
	@Test
	public void testAddHeader() throws Exception {
		// if version is not bumped this should fail
		Verifier verifier = buildBaselineProject("add-header", true);
		verifyBaselineProblem(verifier, "ADDED", "HEADER", "NewHeader:not in the baseline", "1.0.0", "1.0.1");
	}

	/// Helper methods for baseline verifications ///

	private void verifyBaselineProblem(Verifier verifier, String delta, String type, String name, String projectVersion,
			String suggestVersion) throws VerificationException {
		verifyTextInLogMatches(verifier,
				Pattern.compile("\\[ERROR\\].*" + delta + ".*" + type + ".*" + Pattern.quote(name)));
		verifier.verifyTextInLog("Baseline problems found! Project version: " + projectVersion
				+ ", baseline version: 1.0.0, suggested version: " + suggestVersion);
	}

	private Verifier buildBaselineProject(String project, boolean compareShouldFail, String... xargs) throws Exception {
		Verifier verifier = getBaselineProject(project);
		verifier.addCliOption("-Dbaseline-url=" + baselineRepo.toURI());
		for (String xarg : xargs) {
			verifier.addCliOption(xarg);
		}
		if (compareShouldFail) {
			assertThrows(VerificationException.class, () -> verifier.executeGoals(List.of("clean", "verify")));
			verifier.verifyTextInLog("Baseline problems found!");
		} else {
			verifier.executeGoals(List.of("clean", "verify"));
		}
		return verifier;
	}

	private File buildBaseRepo() throws Exception, VerificationException {
		Verifier verifier = getBaselineProject("base-repo");
		verifier.addCliOption("-Dtycho.baseline.skip=true");
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File repoBase = new File(verifier.getBasedir(), "base-repo/site/target/repository");
		assertTrue("base repository was not created at " + repoBase.getAbsolutePath(), repoBase.isDirectory());
		assertTrue("content.jar was not created at " + repoBase.getAbsolutePath(),
				new File(repoBase, "content.jar").isFile());
		assertTrue("artifacts.jar was not created at " + repoBase.getAbsolutePath(),
				new File(repoBase, "artifacts.jar").isFile());
		return repoBase;
	}

	private Verifier getBaselineProject(String project) throws Exception {
		Verifier verifier = getVerifier("baseline", false, true);
		verifier.addCliOption("-f");
		verifier.addCliOption(project + "/pom.xml");
		return verifier;
	}
}
