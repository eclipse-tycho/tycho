/*******************************************************************************
 * Copyright (c) 2011, 2022 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.symlinks;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.BeforeClass;
import org.junit.Test;

public class SymbolicLinkTest extends AbstractTychoIntegrationTest {

	private static final String BASEDIR = "/symLink";
	private static final String LINK_NAME = "linkToProject";

	private static Verifier verifier;

	@BeforeClass
	public static void checkLinkCreation() throws Exception {
		SymbolicLinkTest instance = new SymbolicLinkTest();

		verifier = instance.getVerifier(BASEDIR, false);
		// Note that calling getBaseDir() will trigger copying the test project
		File projectDir = new File(verifier.getBasedir(), "project");
		File linkToProjectDir = new File(projectDir.getParentFile(), LINK_NAME);

		Files.createSymbolicLink(linkToProjectDir.toPath(), projectDir.toPath());
		assertTrue(new File(linkToProjectDir, "pom.xml").isFile());
	}

	@Test
	public void testBuildWithSymbolicLinkOnProjectPath() throws Exception {
		verifier.getCliOptions().addAll(List.of("-f", LINK_NAME + "/pom.xml"));
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

}
