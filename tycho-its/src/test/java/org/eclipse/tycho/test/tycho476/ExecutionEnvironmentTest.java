/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.tycho476;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class ExecutionEnvironmentTest extends AbstractTychoIntegrationTest {

	@Test
	public void testCompilerSourceTargetConfigurationViaManifest() throws Exception {
		Verifier verifier = getVerifier("TYCHO476", false);
		verifier.executeGoal("compile");
		// compile only succeeds with source level 1.6 which
		// is configured indirectly via Bundle-RequiredExecutionEnvironment: JavaSE-1.6
		verifier.verifyErrorFreeLog();
		File classFile = new File(verifier.getBasedir(), "target/classes/TestRunnable.class");
		Assert.assertTrue(classFile.canRead());
		// bytecode major level 61 == target 17
		Assert.assertEquals(61, readClassFileMajorVersion(classFile));
	}

	private static int readClassFileMajorVersion(File classFile) throws Exception {
		try (DataInputStream in = new DataInputStream(new FileInputStream(classFile))) {
			Assert.assertEquals("not a class file: " + classFile, 0xCAFEBABE, in.readInt());
			in.readUnsignedShort(); // minor version
			return in.readUnsignedShort();
		}
	}

}
