/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - inital API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.junit.Test;

public class RepositoryIncludeTest extends AbstractTychoIntegrationTest {

	@Test
	public void testFilterProvided() throws Exception {
		Verifier verifier = getVerifier("p2Repository.filter", false, true);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
		P2RepositoryTool p2Repo = P2RepositoryTool
				.forEclipseRepositoryModule(new File(verifier.getBasedir(), "repository"));
		p2Repo.getUniqueIU("bundle");
		p2Repo.assertNumberOfUnits(1, u -> u.id.equals("a.jre.javase") || u.id.endsWith(".test.category"));
		assertTrue("Bundle artifact not found!", p2Repo.findBundleArtifact("bundle").isPresent());
		p2Repo.assertNumberOfBundles(1);
		p2Repo.assertNumberOfFeatures(0);
	}

}
