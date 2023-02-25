/*******************************************************************************
 * Copyright (c) 2022 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.eclipserun;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesRegex;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class EclipseRunBundleStartLevelTest extends AbstractTychoIntegrationTest {

	@Test
	public void testBundleStartLevel() throws Exception {
		Verifier verifier = getVerifier("eclipserun.bundleStartLevel", true);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
		String config = Files
				.readString(Path.of(verifier.getBasedir(), "target", "eclipserun-work", "configuration", "config.ini"));
		assertThat("org.apache.ant should be auto-started at level 4", config,
				matchesRegex("(?s).*org\\.apache\\.ant_1\\.10\\.12\\.v20211102-1452(\\.jar)?@4\\\\:start.*"));
		assertThat("osgi.bundles.defaultStartLevel should be 5", config,
				matchesRegex("(?s).*osgi.bundles.defaultStartLevel=5.*"));
	}

}
