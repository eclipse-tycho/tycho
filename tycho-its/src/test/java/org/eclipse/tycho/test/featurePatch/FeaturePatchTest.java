/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.featurePatch;

import static org.eclipse.tycho.test.util.ResourceUtil.P2Repositories.ECLIPSE_342;
import static org.eclipse.tycho.test.util.ResourceUtil.P2Repositories.ECLIPSE_352;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class FeaturePatchTest extends AbstractTychoIntegrationTest {

	@Test
	public void testFeaturePatch() throws Exception {
		Verifier verifier = getVerifier("featurePatch.build", false);

		verifier.addCliArgument("-De342-url=" + ECLIPSE_342.toString());
		verifier.addCliArgument("-De352-url=" + ECLIPSE_352.toString());

		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	}
}
