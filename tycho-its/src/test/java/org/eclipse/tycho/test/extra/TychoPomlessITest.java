/*******************************************************************************
 * Copyright (c) 2015, 2019 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *     Christoph Läubrich - add testPomlessFlatBuildExtension
 *******************************************************************************/
package org.eclipse.tycho.test.extra;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.jupiter.api.Test;

public class TychoPomlessITest extends AbstractTychoIntegrationTest {

	@Test
	public void testPomlessBuildExtension() throws Exception {
		Verifier verifier = getVerifier("extra/testpomless", false);
		verifier.executeGoals(asList("clean", "verify"));
		verifier.verifyErrorFreeLog();
		// sanity check pom-less if bundle, test bundle and feature have been built
		check(new File(verifier.getBasedir()));

	}

	private void check(File baseDir) {
		assertIsFile(new File(baseDir, "bundle1/target/pomless.bundle-0.1.0-SNAPSHOT.jar"));
		assertIsFile(new File(baseDir, "bundle1.tests/target/pomless.bundle.tests-1.0.1.jar"));
		assertIsFile(new File(baseDir, "feature/target/pomless.feature-1.0.0-SNAPSHOT.jar"));
		assertIsFile(new File(baseDir, "product/target/my.test.product.pomless-1.0.0.zip"));
		isRepository(baseDir, "product");
		assertIsFile(new File(baseDir, "site1/target/site1.eclipse-repository-0.0.1-SNAPSHOT.zip"));
		isRepository(baseDir, "site1");
	}

	@Test
	public void testPomlessFlatBuildExtension() throws Exception {
		Verifier verifier = getVerifier("extra/testpomless-flat", false);
		verifier.addCliOption("-f");
		verifier.addCliOption("aggregate/pom.xml");
		verifier.executeGoals(asList("clean", "verify"));
		verifier.verifyErrorFreeLog();
		// sanity check pom-less if bundle, test bundle and feature have been built
		check(new File(verifier.getBasedir()));

	}

	@Test
	public void testPomlessStructuredBuildExtension() throws Exception {
		Verifier verifier = getVerifier("extra/testpomless-structured", false);
		verifier.executeGoals(asList("clean", "verify"));
		verifier.verifyErrorFreeLog();
		// sanity check pom-less if bundle, test bundle and feature have been built
		File baseDir = new File(verifier.getBasedir());
		assertIsFile(new File(baseDir, "bundles/bundle1/target/pomless.bundle-0.1.0-SNAPSHOT.jar"));
		assertIsFile(new File(baseDir, "tests/bundle1.tests/target/pomless.bundle.tests-1.0.1.jar"));
		assertIsFile(new File(baseDir, "features/feature/target/pomless.feature-1.0.0-SNAPSHOT.jar"));
		assertIsFile(new File(baseDir, "releng/product/target/my.test.product.pomless-1.0.0.zip"));
		isRepository(baseDir, "releng/product");
		assertIsFile(new File(baseDir, "releng/site1/target/site1.eclipse-repository-0.0.1-SNAPSHOT.zip"));
		isRepository(baseDir, "releng/site1");

	}

	private void isRepository(File baseDir, String subdir) {
		assertIsFile(new File(baseDir, subdir + "/target/repository/artifacts.jar"));
		assertIsFile(new File(baseDir, subdir + "/target/repository/content.jar"));
	}

	private static void assertIsFile(File file) {
		assertTrue(file.isFile(), "expected an existing file: " + file);
	}

}
