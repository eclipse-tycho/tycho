/*******************************************************************************
 * Copyright (c) 2020, 2021 Guillaume Dufour and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Guillaume Dufour - exclude from jar
 *******************************************************************************/

package org.eclipse.tycho.test.compiler;

import java.io.File;
import java.util.zip.ZipFile;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class CompilerExcludeTest extends AbstractTychoIntegrationTest {

	@Test
	public void testExtraExports() throws Exception {
		Verifier verifier = getVerifier("compiler.exclude", false);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		try (ZipFile zip = new ZipFile(new File(verifier.getBasedir(), "mycodelib.jar"))) {
			Assert.assertNotNull(zip.getEntry("exclude/Activator.class"));
			Assert.assertNull(zip.getEntry("exclude/filetoexlude.txt"));
		}
	}

}
