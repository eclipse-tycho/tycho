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
package org.eclipse.tycho.test.resolver;

import java.util.List;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

// tests the various use cases of extraRequirements (bug 363331)
public class ExtraCompilerRequirementsTest extends AbstractTychoIntegrationTest {

	@Test
	public void testArtificial() throws Exception {
		// TODO this is an artificial test case - find a test closer to a real use case
		// TODO avoid remote repositories
		Verifier verifier = getVerifier("/resolver.extraRequirements/artificial", true);
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testDynamicImportPackage() throws Exception {
		Verifier verifier = getVerifier("/resolver.extraRequirements/dynamicimport-package", true);
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testFragmentSplitPackage() throws Exception {
		Verifier verifier = getVerifier("/resolver.extraRequirements/fragment-split-package", true);
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testFragmentImportPackage() throws Exception {
		Verifier verifier = getVerifier("/resolver.extraRequirements/implicit-fragment-import-package", true);
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testImportPackageDirectives() throws Exception {
		Verifier verifier = getVerifier("/resolver.extraRequirements/import-package-directives", true);
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
	}
}
