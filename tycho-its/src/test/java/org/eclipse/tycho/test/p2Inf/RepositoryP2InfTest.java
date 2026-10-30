/*******************************************************************************
 * Copyright (c) 2026 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.p2Inf;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.junit.jupiter.api.Test;

public class RepositoryP2InfTest extends AbstractTychoIntegrationTest {

	@Test
	public void testP2InfInEclipseRepository() throws Exception {
		Verifier verifier = getVerifier("p2Inf.repository", false);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();

		P2RepositoryTool repository = P2RepositoryTool.forEclipseRepositoryModule(new File(verifier.getBasedir()));
		List<String> units = repository.getAllUnitIds();
		assertTrue(units.contains("p2inf.repository.custom.unit"),
				"Unit from p2.inf is missing in repository: " + units);
	}
}
