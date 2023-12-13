/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
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

package org.eclipse.tycho.test.compiler;

import static org.junit.Assert.assertThrows;

import java.util.List;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class MavenCompilerPluginTest extends AbstractTychoIntegrationTest {

	@Test
	public void testJDTCompilerId() throws Exception {
		Verifier verifier = getVerifier("compiler.mavenCompilerPlugin", false);
		assertThrows(VerificationException.class, () -> verifier.executeGoal("compile"));
		// expected
		verifier.verifyTextInLog("field Foo.unused is not used");
	}

	@Test
	public void testAdditionalBundles() throws Exception {
		Verifier verifier = getVerifier("compiler.additional.bundles", true);
		verifier.executeGoal("compile");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testAdditionalBundles2() throws Exception {
		Verifier verifier = getVerifier("compiler.additional.bundles2", false, false);
		verifier.executeGoals(List.of("clean", "install"));
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testLimitModules() throws Exception {
		Verifier verifier = getVerifier("compiler.limit.modules", true);
		verifier.executeGoal("compile");
		verifier.verifyErrorFreeLog();
	}

}
