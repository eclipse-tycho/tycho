/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.target;

import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

/**
 * Unmapped p2 units get a synthetic {@code p2.*} groupId, mapped ones get the coordinates from the p2 metadata. Visible in {@code dependency:list}.
 */
public class P2MavenMetadataHandlingTest extends AbstractTychoIntegrationTest {

	// groupId used when a unit is not mapped, see TychoConstants.P2_GROUPID_PREFIX
	private static final String UNMAPPED_COORDINATES = "p2.eclipse.plugin:org.eclipse.osgi";

	@Test
	public void injectMapsUnitsToTheirMavenCoordinates() throws Exception {
		Verifier verifier = buildWithHandling("inject");
		// mapped, so the p2 groupId is gone
		verifyTextNotInLog(verifier, UNMAPPED_COORDINATES);
	}

	@Test
	public void ignoreKeepsUnitsUnmapped() throws Exception {
		Verifier verifier = buildWithHandling("ignore");
		// not mapped, so the p2 groupId stays
		verifier.verifyTextInLog(UNMAPPED_COORDINATES);
	}

	private Verifier buildWithHandling(String handling) throws Exception {
		Verifier verifier = getVerifier("target.p2MavenMetadataHandling", true);
		verifier.addCliOption("-Dhandling=" + handling);
		verifier.executeGoals(List.of("verify", "dependency:list"));
		verifier.verifyErrorFreeLog();
		return verifier;
	}
}
