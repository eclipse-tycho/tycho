/*******************************************************************************
 * Copyright (c) 2011 SAP AG. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class JavaToolchainInSurefireTest extends AbstractTychoIntegrationTest {
	@Test
	public void test() throws Exception {
		Verifier verifier = getVerifier("surefire.toolchains");
		File toolchains = new File(verifier.getBasedir() + "/toolchains.xml");
		verifier.addCliArguments("--toolchains", toolchains.getCanonicalPath());
		verifier.executeGoal("integration-test");
		verifier.verifyTextInLog("Toolchain in tycho-surefire-plugin: JDK[fake-jdk-home]");
	}
}
