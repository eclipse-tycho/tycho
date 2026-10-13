/*******************************************************************************
 * Copyright (c) 2026 Sigasi and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sigasi - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.test.packaging;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.util.jar.JarFile;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.jupiter.api.Test;

public class UseDefaultExcludesTest extends AbstractTychoIntegrationTest {

	@Test
	public void testDefaultExcludesRemoveGitignoreByDefault() throws Exception {
		try (JarFile jarFile = buildAndGetPluginJar("enabled")) {
			assertNotNull(jarFile.getEntry("testdata/file.txt"), "testdata/file.txt is missing from " + jarFile.getName());
			assertNull(jarFile.getEntry("testdata/.gitignore"),
					"testdata/.gitignore should have been excluded from " + jarFile.getName());
		}
	}

	@Test
	public void testDisabledDefaultExcludesKeepGitignore() throws Exception {
		try (JarFile jarFile = buildAndGetPluginJar("disabled")) {
			assertNotNull(jarFile.getEntry("testdata/file.txt"), "testdata/file.txt is missing from " + jarFile.getName());
			assertNotNull(jarFile.getEntry("testdata/.gitignore"), "testdata/.gitignore is missing from " + jarFile.getName());
		}
	}

	private JarFile buildAndGetPluginJar(String project) throws Exception {
		Verifier verifier = getVerifier("packaging.useDefaultExcludes/" + project, false);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
		File jar = new File(verifier.getBasedir(), "target/useDefaultExcludes." + project + "-1.0.0-SNAPSHOT.jar");
		return new JarFile(jar);
	}

}
