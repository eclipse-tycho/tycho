/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.resolver;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class ResolverTests extends AbstractTychoIntegrationTest {

	private static final String SDK_23 = "org.eclipse.swt.gtk.linux.x86_64 3.119.0.v20220223-1102";
	private static final String SDK_22 = "org.eclipse.swt.gtk.linux.x86_64 3.118.0.v20211123-0851";
	private static final String SDK_21 = "org.eclipse.swt.gtk.linux.x86_64 3.117.0.v20210906-0842";

	/**
	 * This test case tests a combination that at a first glance looks very simple
	 * but is hard to resolve due to the structure of 'org.eclipse.core.runtime'
	 * bundle. One can force a failure by running the following commandline
	 * 
	 * <pre>
	 * mvn clean install -Dtycho.equinox.resolver.batch.size=1 -Dtycho.equinox.resolver.uses=true
	 * </pre>
	 * 
	 * what then fails with: Bundle was not resolved because of a uses constraint
	 * violation, so this test effectively ensures that the defaults are working
	 * without any special options
	 * 
	 * @throws Exception if anything goes wrong
	 */
	@Test
	public void testUsesConstraintViolations() throws Exception {

		Verifier verifier = getVerifier("resolver.usesConstraintViolations");
		verifier.executeGoal("compile");
		verifier.verifyErrorFreeLog();
	}

	/**
	 * This test case test a combination of plain maven build artifacts (using
	 * felix-bundle-plugin) and tycho eclipse-plugin build artifact.
	 * 
	 * @throws Exception if anything goes wrong
	 */
	@Test
	public void testMixedReactor() throws Exception {

		Verifier verifier = getVerifier("mixed.reactor", false);
		// FIXME see Issue #479 // verifier.executeGoal("compile");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testMultipleFragmentsOnlyOneIsChoosen() throws Exception {

		Verifier verifier = getVerifier("resolver.multipleDownloads", false, false);
		verifier.executeGoal("compile");
		verifier.verifyErrorFreeLog();
		List<String> lines = verifier.loadFile(verifier.getBasedir(), verifier.getLogFileName(), false);

//        	[INFO] Resolved fragments:
//       		[INFO]  org.eclipse.swt.gtk.linux.x86_64 3.118.0.v20211123-0851
//       		[INFO]  org.eclipse.swt.gtk.linux.x86_64 3.119.0.v20220223-1102
//       		[INFO]  org.eclipse.swt.gtk.linux.x86_64 3.117.0.v20210906-0842
//       		[INFO] ------------------------------------------------------------------------
		boolean startLine = false;
		boolean highestVersionFound = false;
		for (String ansiLine : lines) {
			String line = Verifier.stripAnsi(ansiLine);
			if (startLine) {
				if (line.endsWith("------")) {
					break;
				}
				if (line.endsWith(SDK_21)) {
					fail("3.117 was found but should not be part of the result");
				}
				if (line.endsWith(SDK_22)) {
					fail("3.118 was found but should not be part of the result");
				}
				highestVersionFound |= line.endsWith(SDK_23);
			} else {
				startLine = line.endsWith("Resolved fragments:");
			}
		}
		assertTrue("Start line not found!", startLine);
		assertTrue("Highest version was not found", highestVersionFound);
	}

	@Test
	public void testConsiderResolutionWithUsesDirectiveIfVanillaResolutionFails() throws Exception {
		Verifier verifier = getVerifier("resolver.usesNecessary", false, false);
		verifier.executeGoal("compile");
		verifier.verifyErrorFreeLog();
	}
}
