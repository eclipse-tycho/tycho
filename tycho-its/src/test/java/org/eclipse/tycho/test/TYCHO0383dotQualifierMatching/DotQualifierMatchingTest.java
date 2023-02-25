/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.TYCHO0383dotQualifierMatching;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class DotQualifierMatchingTest extends AbstractTychoIntegrationTest {
	@Test
	public void testFeature() throws Exception {
		Verifier verifier = getVerifier("/TYCHO0383dotQualifierMatching/featureDotQualifier", false);
		verifier.addCliArgument("-Dp2.repo=" + P2Repositories.ECLIPSE_342.toString());
		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();

		assertFileExists(new File(verifier.getBasedir()),
				"target/site/plugins/org.eclipse.osgi_3.4.3.R34x_v20081215-1030.jar");
	}

	@Test
	public void testProduct() throws Exception {
		Verifier verifier = getVerifier("/TYCHO0383dotQualifierMatching/productDotQualifier", false);
		verifier.addCliArgument("-Dp2.repo=" + P2Repositories.ECLIPSE_342.toString());
		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();

		assertFileExists(new File(verifier.getBasedir()),
				"productDotQualifier.product/target/repository/plugins/org.eclipse.osgi_3.4.3.R34x_v20081215-1030.jar");
	}

}
