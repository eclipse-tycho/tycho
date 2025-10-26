/*******************************************************************************
 * Copyright (c) 2025 Contributors and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Contributors - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.eeProfile;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("unless java 25 jvm is available to tycho build")
public class Java25ResolutionTest extends AbstractTychoIntegrationTest {

	@Test
	public void testBundleBuildForJava25() throws Exception {
		// Test that a bundle with JavaSE-25 BREE can be built with javac compiler
		Verifier verifier = getVerifier("eeProfile.java25", false);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		verifier.verifyTextInLog("Building jar:");
		File buildResult = new File(verifier.getBasedir());
		File bundleJar = new File(buildResult, "bundle/target/java25.bundle-1.0.0-SNAPSHOT.jar");
		assertTrue("Bundle JAR should exist", bundleJar.exists());
	}

}
