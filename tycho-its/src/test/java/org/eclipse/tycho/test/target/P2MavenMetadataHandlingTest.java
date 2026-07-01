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

import static org.junit.Assert.assertThrows;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class P2MavenMetadataHandlingTest extends AbstractTychoIntegrationTest {

	@Test
	public void ignoreSkipsMapping() throws Exception {
		Verifier verifier = getVerifier("target.p2MavenMetadataHandling", false);
		verifier.addCliOption("-Dp2.repo=" + P2Repositories.ECLIPSE_LATEST.toString().replace("/", "//"));
		verifier.addCliOption("-Dhandling=ignore");
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		verifyTextNotInLog(verifier, "Mapping P2 > Maven Coordinates");
	}

	@Test
	public void invalidValueIsRejected() throws Exception {
		Verifier verifier = getVerifier("target.p2MavenMetadataHandling", false);
		verifier.addCliOption("-Dp2.repo=" + P2Repositories.ECLIPSE_LATEST.toString().replace("/", "//"));
		verifier.addCliOption("-Dhandling=bogus");
		assertThrows(VerificationException.class, () -> verifier.executeGoal("verify"));
		verifier.verifyTextInLog("Illegal value of <p2MavenMetadataHandling>");
	}
}
