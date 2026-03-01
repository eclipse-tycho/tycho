/*******************************************************************************
 * Copyright (c) 2025 Contributors to the Eclipse Foundation and others.
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

import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.EnvironmentUtil;
import org.junit.Test;

public class SbomPluginTest extends AbstractTychoIntegrationTest {

	@Test
	public void testBuildWithProfile() throws Exception {
		Verifier verifier = getVerifier("sbom-simple-product", false);
		verifier.addCliArgument("-Psbom-generation");
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
		Path basedir = Path.of(verifier.getBasedir());
		assertTrue("sbom.xml is missing!", Files.isRegularFile(basedir.resolve("target/sbom-simple-product.xml")));
		assertTrue("sbom.json is missing!", Files.isRegularFile(basedir.resolve("target/sbom-simple-product.json")));
	}

	@Test
	public void testCLIInvocation() throws Exception {
		Verifier verifier = getVerifier("sbom-simple-product", true);
		verifier.executeGoals(List.of("clean", "package"));
		verifier.setAutoclean(false);
		verifyErrorFreeLog(verifier);
		// Execute in the maven project itself
		String installation = "target/products/product.uid/linux/gtk/x86_64";
		verifyCLI(verifier, installation);
		// execute in an empty folder
		Verifier cli = getVerifier("cli", false);
		cli.setAutoclean(false);
		verifyCLI(cli, Path.of(verifier.getBasedir()).resolve(installation).toString());
	}

	private void verifyCLI(Verifier verifier, String installation) throws VerificationException {
		verifier.executeGoals(List.of(
				"org.eclipse.tycho:tycho-sbom-plugin:" + EnvironmentUtil.getProperty("tycho-version") + ":generator",
				"-Dinstallation=" + installation, "-Dprint.xml"));
		verifyErrorFreeLog(verifier);
		verifier.verifyTextInLog("<bom "); // not very smart but should make sure we actually run it
	}
}
