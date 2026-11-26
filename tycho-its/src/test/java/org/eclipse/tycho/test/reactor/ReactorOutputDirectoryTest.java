/*******************************************************************************
 * Copyright (c) 2026 Chris Lake and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.reactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * A sibling bundle that was compiled by an earlier build, but is not built in the current session,
 * must resolve to its output directory and not to the project root.
 */
public class ReactorOutputDirectoryTest extends AbstractTychoIntegrationTest {

	@Test
	public void unpackagedSiblingResolvesToOutputDirectory() throws Exception {
		Verifier verifier = getVerifier("reactor.outputDirectory", false);
		verifier.executeGoal("compile");
		verifier.verifyErrorFreeLog();

		// New session that runs no lifecycle, so no module has a main artifact file
		verifier.setAutoclean(false);
		File classpathFile = new File(verifier.getBasedir(), "client/target/classpath.txt");
		verifier.addCliOption("-Dmdep.outputFile=" + classpathFile.getAbsolutePath());
		verifier.addCliOption("-pl");
		verifier.addCliOption("client");
		verifier.addCliOption("-am");
		verifier.executeGoal("org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath");
		verifier.verifyErrorFreeLog();

		List<String> entries = Arrays
				.asList(Files.readString(classpathFile.toPath()).trim().split(File.pathSeparator));
		File apiBasedir = new File(verifier.getBasedir(), "api");
		File apiClasses = new File(apiBasedir, "target/classes");
		assertTrue(entries.contains(apiClasses.getAbsolutePath()),
				"api output directory expected on classpath, but was " + entries);
		assertEquals(0, entries.stream().filter(e -> new File(e).equals(apiBasedir)).count(),
				"project root of api must not be on the classpath: " + entries);
	}
}
