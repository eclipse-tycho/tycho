/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.mngeclipse1007;

import java.io.File;
import java.util.zip.ZipFile;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class BinExcludedTest extends AbstractTychoIntegrationTest {

	@Test
	public void test() throws Exception {
		Verifier verifier = getVerifier("MNGECLIPSE1007");

		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		try (ZipFile zip = new ZipFile(new File(verifier.getBasedir(), "target/MNGECLIPSE1007-1.0.0.jar"))) {
			Assert.assertNotNull(zip.getEntry("files/included.txt"));
			Assert.assertNull(zip.getEntry("files/excluded.txt"));
			Assert.assertNull(zip.getEntry("Makefile"));
		}
	}

}
