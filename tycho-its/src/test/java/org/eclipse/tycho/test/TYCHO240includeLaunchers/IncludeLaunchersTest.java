/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.TYCHO240includeLaunchers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class IncludeLaunchersTest extends AbstractTychoIntegrationTest {

	@Test
	public void includeLaunchers() throws Exception {
		Verifier verifier = getVerifier("/TYCHO240includeLaunchers/includeLaunchers");
		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();

		File targetdir = new File(verifier.getBasedir(), "target");

		// assert product zip was created for each target environment
		assertTrue(
				new File(targetdir, "repository/binary/includedLauncher.executable.gtk.linux.x86_64_1.0.0").canRead());

	}

	@Test
	public void noIncludeLaunchers() throws Exception {
		Verifier verifier = getVerifier("/TYCHO240includeLaunchers/noIncludeLaunchers");
		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();

		File targetdir = new File(verifier.getBasedir(), "target");

		// assert product zip was created for each target environment
		assertFalse(new File(targetdir, "linux.gtk.x86_64/eclipse/libcairo-swt.so").canRead());
		assertFalse(new File(targetdir, "linux.gtk.x86_64/eclipse/includedLauncher").canRead());
	}

}
