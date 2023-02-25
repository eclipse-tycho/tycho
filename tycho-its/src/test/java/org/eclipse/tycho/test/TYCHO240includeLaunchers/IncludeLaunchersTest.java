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
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class IncludeLaunchersTest extends AbstractTychoIntegrationTest {

	@Test
	public void includeLaunchers() throws Exception {
		Verifier verifier = getVerifier("/TYCHO240includeLaunchers/includeLaunchers");
		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();

		File targetdir = new File(verifier.getBasedir(), "target");
		File binaryDir = new File(targetdir, "repository/binary/");
		String executable;
		if (SystemUtils.IS_OS_WINDOWS) {
			executable = "includedLauncher.executable.win32.win32.x86_64_1.0.0";
		} else {
			executable = "includedLauncher.executable.gtk.linux.x86_64_1.0.0";
		}
		File file = new File(binaryDir, executable);
		assertTrue("Directory " + binaryDir.getAbsolutePath() + " is not a directory", binaryDir.isDirectory());
		assertTrue("File " + file.getAbsolutePath() + " do not exits, but " + listFiles(binaryDir), file.isFile());

	}

	private String listFiles(File binaryDir) {
		File[] listFiles = binaryDir.listFiles();
		if (listFiles != null) {
			return Arrays.stream(listFiles).map(File::getName).collect(Collectors.joining(", "));
		}
		return "";
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
