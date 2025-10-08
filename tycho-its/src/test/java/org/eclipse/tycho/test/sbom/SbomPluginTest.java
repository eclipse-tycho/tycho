/*******************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.sbom;

import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class SbomPluginTest extends AbstractTychoIntegrationTest {

	@Test
	public void testBuildWithoutProfile() throws Exception {
		Verifier verifier = getVerifier("sbom-simple-product", false);
		
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testBuildWithProfile() throws Exception {
		Verifier verifier = getVerifier("sbom-simple-product", false);
		
		verifier.addCliOption("-Psbom-generation");
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testCLIInvocation() throws Exception {
		Verifier verifier = getVerifier("sbom-simple-product", false);
		
		verifier.executeGoals(List.of("clean", "verify", "org.eclipse.tycho:tycho-sbom-plugin:generator"));
		verifier.verifyErrorFreeLog();
	}
}
