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

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

/**
 * Demonstrates the {@code <p2MavenMetadataHandling>} target-platform-configuration option, which
 * controls whether p2 units are mapped to (and resolved against) Maven coordinates. Building a
 * bundle against a real p2 target platform with {@code inject} or {@code ignore} lets Tycho skip the
 * per-artifact remote Maven lookup that {@code validate} (the default) performs.
 */
public class P2MavenMetadataHandlingTest extends AbstractTychoIntegrationTest {

	@Test
	public void injectMapsCoordinatesWithoutRemoteResolution() throws Exception {
		buildWithHandling("inject");
	}

	@Test
	public void ignoreSkipsMapping() throws Exception {
		buildWithHandling("ignore");
	}

	private void buildWithHandling(String handling) throws Exception {
		Verifier verifier = getVerifier("target.p2MavenMetadataHandling", true);
		verifier.addCliOption("-Dhandling=" + handling);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	}
}
