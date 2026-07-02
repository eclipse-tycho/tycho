/*******************************************************************************
 * Copyright (c) 2010, 2021 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static org.eclipse.tycho.test.util.ResourceUtil.P2Repositories.ECLIPSE_352;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.BeforeClass;
import org.junit.Test;

public class TransitiveP2RepoTest extends AbstractTychoIntegrationTest {

	private static final String MODULE_NON_TRANSITIVE = "repository-includedOnly";

	private static final String MODULE_TRANSITIVE = "repository-allDependencies";

	private static Verifier verifier;

	@BeforeClass
	public static void buildFeatureAndBundlesAndRepos() throws Exception {
		verifier = new TransitiveP2RepoTest().getVerifier("p2Repository.transitive", false);
		verifier.addCliArgument("-Dp2.repo=" + ECLIPSE_352.toString());
		/*
		 * Do not execute "install" to ensure that features and bundles can be included
		 * directly from the build results of the local reactor.
		 */
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testEclipseRepositoryTransitive() {
		File pluginsDir = new File(verifier.getBasedir(), MODULE_TRANSITIVE + "/target/repository/plugins");
		assertTrue(checkFileWithPrefixExists(pluginsDir, "tycho551.bundle1_"));

		assertTrue(checkFileWithPrefixExists(pluginsDir, "tycho551.bundle2_"));
		assertTrue(checkFileWithPrefixExists(pluginsDir, "tycho551.bundle3_"));
		assertTrue(checkFileWithPrefixExists(pluginsDir, "org.eclipse.osgi_"));
		assertTrue(checkFileWithPrefixExists(pluginsDir, "org.junit_"));
	}

	@Test
	public void testEclipseRepositoryNonTransitive() {
		File pluginsDir = new File(verifier.getBasedir(), MODULE_NON_TRANSITIVE + "/target/repository/plugins");
		assertTrue(checkFileWithPrefixExists(pluginsDir, "tycho551.bundle1_"));

		assertFalse(checkFileWithPrefixExists(pluginsDir, "tycho551.bundle2_"));
		assertFalse(checkFileWithPrefixExists(pluginsDir, "tycho551.bundle3_"));
		assertFalse(checkFileWithPrefixExists(pluginsDir, "org.eclipse.osgi_"));
		assertFalse(checkFileWithPrefixExists(pluginsDir, "org.junit_"));
	}

	private boolean checkFileWithPrefixExists(File dir, String prefix) {
		File[] files = dir.listFiles();
		if (files == null) {
			return false;
		}
		for (File file : files) {
			if (file.isFile() && file.getName().startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}
}
