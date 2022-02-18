/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.TYCHO253extraClassPathEntries;

import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class ExtraClassPathEntriesTest extends AbstractTychoIntegrationTest {
	@Test
	public void testJarsExtraClasspath() throws Exception {
		Verifier verifier = getVerifier("/TYCHO253extraClassPathEntries/org.eclipse.tycho.testExtraClasspathTest1");
		verifier.executeGoals(List.of("clean", "install"));
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testExtraClasspath() throws Exception {
		Verifier verifier = getVerifier("/TYCHO253extraClassPathEntries/org.eclipse.tycho.testExtraClasspathTest2");
		verifier.executeGoals(List.of("clean", "install"));
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testReferenceToInnerJar() throws Exception {
		Verifier verifier = getVerifier("/TYCHO253extraClassPathEntries");
		verifier.executeGoals(List.of("clean", "install"));
		verifier.verifyErrorFreeLog();
	}
}
