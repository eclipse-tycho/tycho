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
package org.eclipse.tycho.test.surefire;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class ExplodedTestDependenciesTest extends AbstractTychoIntegrationTest {

	@Test
	public void testLocalMavenRepository() throws Exception {
		// project that marks org.apache.ant as "exploded" (unpacked) for the test
		// runtime -> supported since TYCHO-340
		Verifier v01 = getVerifier("surefire.bundleUnpack", true);
		v01.executeGoal("install");
		v01.verifyErrorFreeLog();
		// TODO this is only an indirect test; it should test that the bundles nested
		// jars are accessible as file URLs
		File antHome = new File(v01.getBasedir(),
				"tycho340.test/target/work/plugins/org.apache.ant_1.10.12.v20211102-1452");
		Assert.assertTrue(antHome.isDirectory());
	}

}
