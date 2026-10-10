/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.issue6251;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.jupiter.api.Test;

public class Issue6251Test extends AbstractTychoIntegrationTest {

	@Test
	public void modifyCompositeRepositoryInPlaceWritesP2Index() throws Exception {
		Verifier verifier = getVerifier("issue6251");

		verifier.executeGoals(List.of("initialize"));
		verifier.verifyErrorFreeLog();

		Path p2Index = Path.of(verifier.getBasedir(), "repo", "p2.index");
		assertTrue(Files.exists(p2Index), "p2.index was not written to the in-place repository");
	}
}
