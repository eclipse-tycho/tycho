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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("unless java 25 jvm is available to tycho build")
public class Java25ResolutionTest extends AbstractTychoIntegrationTest {

	private static File buildResult;

	@BeforeClass
	public static void setUp() throws Exception {
		Verifier verifier = new Java25ResolutionTest().getVerifier("eeProfile.java25", false);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		buildResult = new File(verifier.getBasedir());
	}

	@Test
	public void testProductBuildForJava25() throws Exception {
		// a p2 repository that contains a product for Java 25
		P2RepositoryTool productRepo = P2RepositoryTool.forEclipseRepositoryModule(new File(buildResult, "repository"));
		List<String> jreUnitVersions = productRepo.getUnitVersions("a.jre.javase");
		// we expect java 25
		assertThat(jreUnitVersions, hasItem("25.0.0"));
	}

}
