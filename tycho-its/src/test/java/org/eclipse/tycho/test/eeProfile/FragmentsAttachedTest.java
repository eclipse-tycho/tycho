/*******************************************************************************
 * Copyright (c) 2014, 2021 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.eeProfile;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class FragmentsAttachedTest extends AbstractTychoIntegrationTest {

	@Test
	public void testSWTFragmentsAttached() throws Exception {
		Verifier verifier = getVerifier("eeProfile.resolution.fragments", false);

		verifier.executeGoal("verify");

		verifier.verifyErrorFreeLog();
	}

}
