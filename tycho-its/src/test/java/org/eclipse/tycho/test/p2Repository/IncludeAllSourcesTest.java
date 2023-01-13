/*******************************************************************************
 * Copyright (c) 2023 Red Hat, Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.junit.BeforeClass;
import org.junit.Test;

public class IncludeAllSourcesTest extends AbstractTychoIntegrationTest {

	private static Verifier verifier;
	private static P2RepositoryTool p2Repo;

	@BeforeClass
	public static void executeBuild() throws Exception {
		verifier = new IncludeAllSourcesTest().getVerifier("/p2Repository.includeAllSources", false);
		verifier.executeGoal("verify");
		// Missing source should never trigger an error
		verifier.verifyErrorFreeLog();
		p2Repo = P2RepositoryTool.forEclipseRepositoryModule(new File(verifier.getBasedir()));
	}

	@Test
	public void testSourceInclusionDirectlyReferenced() throws Exception {
		assertNotNull(p2Repo.getUniqueIU("org.apache.batik.css.source"));
	}

	@Test
	public void testSourceInclusionTransitiveDeps() throws Exception {
		assertNotNull(p2Repo.getUniqueIU("org.w3c.css.sac.source"));
	}

	@Test(expected = AssertionError.class)
	public void testAllowMissingSource() throws Exception {
		p2Repo.getUniqueIU("org.apache.commons.commons-io.source");
	}
}
