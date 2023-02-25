/*******************************************************************************
 * Copyright (c) 2021, 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.compiler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.EnvironmentUtil;
import org.junit.Test;

public class CompilerClasspathEntryTest extends AbstractTychoIntegrationTest {

	@Test
	public void testJUnit4Container() throws Exception {
		Verifier verifier = getVerifier("compiler.junitcontainer/junit4-in-bundle", true);
		verifier.executeGoal("test");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testJUnit4ContainerWithDependencies() throws Exception {
		Verifier verifier = getVerifier("compiler.junitcontainer/junit4-in-bundle-with-dependencies", true);
		verifier.executeGoal("test");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testLibEntry() throws Exception {
		Verifier verifier = getVerifier("compiler.libentry/my.bundle", false);
		verifier.executeGoal("compile");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testDSComponents() throws Exception {
		Verifier verifier = getVerifier("tycho-ds", false, true);
		verifier.setSystemProperty("repo-url", EnvironmentUtil.ECLIPSE_LATEST);
		// first test to consume from target platform
		verifyDs(verifier);
		// now test consume from maven directly
		verifier.addCliOption("-Pfiltered");
		verifyDs(verifier);
	}

	private void verifyDs(Verifier verifier) throws VerificationException {
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
		File generated = new File(verifier.getBasedir(), "target/classes/OSGI-INF");
		assertTrue(new File(generated, "tycho.ds.TestComponent.xml").isFile());
		assertFalse(new File(generated, "tycho.ds.TestComponent2.xml").isFile());
	}

	@Test
	public void testOSGiAnnotations() throws Exception {
		Verifier verifier = getVerifier("compiler.annotations", false, true);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		File dependencies = new File(verifier.getBasedir(), "target/dependencies-list.txt");
		assertTrue(dependencies.getAbsoluteFile() + " not found!", dependencies.isFile());
		List<String> lines = Files.readAllLines(dependencies.toPath());
		String collect = lines.stream().collect(Collectors.joining(",\r\n"));
		// TODO we should possibly accept others that supply the ds annotations here?
		assertTrue("org.eclipse.osgi.services not found in dependencies: " + collect,
				lines.stream().anyMatch(s -> s.contains("org.eclipse.osgi.services")));
		assertTrue("org.osgi.annotation.bundle not found in dependencies: " + collect,
				lines.stream().anyMatch(s -> s.contains("org.osgi.annotation.bundle")));
		assertTrue("org.osgi.annotation.versioning not found in dependencies: " + collect,
				lines.stream().anyMatch(s -> s.contains("org.osgi.annotation.versioning")));
	}

}
