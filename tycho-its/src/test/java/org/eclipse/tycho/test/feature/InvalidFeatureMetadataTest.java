/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.feature;

import static org.junit.Assert.assertThrows;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class InvalidFeatureMetadataTest extends AbstractTychoIntegrationTest {

	@Test
	public void reportsFeatureLocationWhenMetadataCannotBeRead() throws Exception {
		Verifier verifier = getVerifier("feature.invalidMetadata", false);
		assertThrows(VerificationException.class, () -> verifier.executeGoal("package"));
		verifier.verifyTextInLog("Unable to read feature metadata from " + verifier.getBasedir());
	}
}
