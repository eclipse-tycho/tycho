/*******************************************************************************
 * Copyright (c) 2026 Aleksandar Kurtakov and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Aleksandar Kurtakov - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.List;

import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.verifier.Verifier;
import org.junit.jupiter.api.Test;

/**
 * Assembles repositories with and without sources for a bundle that depends on target platform
 * bundles with sources, and checks that only the expected bundles and source bundles end up in
 * them.
 */
public class SourceBundleInclusionTest extends AbstractTychoIntegrationTest {

	private static final String BUNDLE = "sourceBundleInclusion.bundle";

	@Test
	public void testSourceBundleInclusion() throws Exception {
		Verifier verifier = getVerifier("p2Repository.sourceBundleInclusion", false);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		File basedir = new File(verifier.getBasedir());

		assertEquals(List.of(BUNDLE), bundles(basedir, "no-sources"));

		// the sources of the bundle's dependencies that aren't part of the repository must not be added
		assertEquals(List.of(BUNDLE), bundles(basedir, "all-sources-bundle-only"));
		assertEquals(List.of("org.opentest4j", "org.opentest4j.source", BUNDLE), bundles(basedir, "all-sources"));

		// an explicitly included source bundle must not bring in the sources of other bundles
		assertEquals(List.of("org.opentest4j.source", BUNDLE), bundles(basedir, "explicit-source"));
	}

	private static List<String> bundles(File basedir, String module) {
		return P2RepositoryTool.forEclipseRepositoryModule(new File(basedir, module)).getBundles()
				.map(file -> file.getName().substring(0, file.getName().lastIndexOf('_'))).sorted().toList();
	}
}
