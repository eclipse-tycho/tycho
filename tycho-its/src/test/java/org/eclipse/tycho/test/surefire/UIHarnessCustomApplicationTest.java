/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Lars Vogel - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Verifies that a UI test runtime with a custom application resolves and starts without the Eclipse
 * IDE bundles, as required by RCP applications.
 */
public class UIHarnessCustomApplicationTest extends AbstractTychoIntegrationTest {

	@Test
	public void testCustomApplicationDoesNotRequireIdeBundles() throws Exception {
		Verifier verifier = getVerifier("surefire.uiHarness.customApplication");
		verifier.addCliOption("-Dtycho.printBundles=true");

		// the configured application does not exist, so the launch fails once the runtime is up
		assertThrows(VerificationException.class, () -> verifier.executeGoal("integration-test"));
		verifier.verifyTextInLog("Could not find application \"tycho.its.uiharness.customapp\"");

		List<String> installedBundles = installedBundles(verifier);
		assertFalse(installedBundles.isEmpty(), "test runtime did not report its installed bundles");
		assertTrue(installedBundles.stream().anyMatch(line -> line.contains("org.eclipse.ui.workbench ")),
				"UI test harness requires org.eclipse.ui.workbench: " + installedBundles);
		assertFalse(installedBundles.stream().anyMatch(line -> line.contains("org.eclipse.ui.ide.application ")),
				"test runtime must not contain the IDE application bundle: " + installedBundles);
	}

	private static List<String> installedBundles(Verifier verifier) throws VerificationException {
		List<String> lines = verifier.loadFile(verifier.getBasedir(), verifier.getLogFileName(), false);
		int start = indexOfLineContaining(lines, "====== Installed Bundles ========", 0);
		if (start < 0) {
			return List.of();
		}
		int end = indexOfLineContaining(lines, "=================================", start + 1);
		return lines.subList(start + 1, end < 0 ? lines.size() : end);
	}

	private static int indexOfLineContaining(List<String> lines, String text, int fromIndex) {
		for (int i = fromIndex; i < lines.size(); i++) {
			if (lines.get(i).contains(text)) {
				return i;
			}
		}
		return -1;
	}
}
