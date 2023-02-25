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
package org.eclipse.tycho.test.multiplatform;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class MultiplatformProductTest extends AbstractTychoIntegrationTest {
	@Test
	public void exportProduct() throws Exception {
		Verifier verifier = getVerifier("multiPlatform.product");
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();

		File targetdir = new File(verifier.getBasedir(), "tycho.demo.rcp/target");

		// assert expanded product folders have proper swt fragments
		assertFileExists(targetdir, "repository/plugins/org.eclipse.swt.gtk.linux.x86_64_*.jar");
		assertFileExists(targetdir, "repository/plugins/org.eclipse.swt.cocoa.macosx.x86_64_*.jar");
		assertFileExists(targetdir, "repository/plugins/org.eclipse.swt.win32.win32.x86_64_*.jar");

		// assert native launchers
		assertTrue(new File(targetdir, "products/tychodemo/linux/gtk/x86_64/tychodemo").canRead());
		assertTrue(new File(targetdir, "products/tychodemo/win32/win32/x86_64/tychodemo.exe").canRead());
		assertTrue(new File(targetdir, "products/tychodemo/macosx/cocoa/x86_64/Eclipse.app/Contents/MacOS/tychodemo")
				.canRead());

		// assert product zip was created for each target environment
		assertFile(new File(targetdir, "products/tychodemo-linux.gtk.x86_64.tar.gz"));
		assertFile(new File(targetdir, "products/tychodemo-macosx.cocoa.x86_64.tar.gz"));
		assertFile(new File(targetdir, "products/tychodemo-win32.win32.x86_64.zip"));
	}

	private void assertFile(File file) {
		assertTrue(file.getAbsolutePath() + " is not a file", file.isFile());
	}

}
