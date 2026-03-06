/*******************************************************************************
 * Copyright (c) 2010, 2021 SAP AG and others.
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
package org.eclipse.tycho.test.TYCHO449SrcIncludesExcludes;

import java.io.File;
import java.util.jar.JarFile;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class Tycho449SrcIncludesExcludesTest extends AbstractTychoIntegrationTest {

	@Test
	public void testDefaultSourceBundleSuffix() throws Exception {
		Verifier verifier = getVerifier("/TYCHO449SrcIncludesExcludes", false);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
		try (JarFile sourceJar = new JarFile(
				new File(verifier.getBasedir(), "target/TestSourceIncludesExcludes-1.0.0-SNAPSHOT-sources.jar"))) {
			Assert.assertNull(sourceJar.getEntry("resourceFolder/.hidden/toBeExcluded.txt"));
			Assert.assertNull(sourceJar.getEntry("resourceFolder/.svn/"));
			Assert.assertNotNull(sourceJar.getEntry("resourceFolder/test.txt"));
			Assert.assertNotNull(sourceJar.getEntry("resource.txt"));
			Assert.assertNotNull(sourceJar.getEntry("additionalResource.txt"));
		}
	}

}
