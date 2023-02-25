/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.product;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWithIgnoringCase;
import static org.junit.Assume.assumeThat;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class MetaRequirementsTest extends AbstractTychoIntegrationTest {

	@Test
	public void testProductInstallationWithCustomTouchpoint() throws Exception {
		// TODO: fix this test for windows
		assumeThat(System.getProperty("os.name"), not(startsWithIgnoringCase("windows")));

		/*
		 * Project building a product distribution which includes a bundle that uses a
		 * custom touchpoint. The implementation of the touchpoint is installed into the
		 * director building the distribution through a p2 metaRequirement. This
		 * requires a p2 director which is itself a p2-updatable installation.
		 * Therefore, a standalone p2 director is created in the target folder before
		 * the actual product materialization. (Tycho's OSGi runtime, which also
		 * includes a director, is intentionally not updatable and hence cannot be
		 * used.)
		 */
		Verifier verifier = getVerifier("product.metaRequirements", false);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		verifier.verifyTextInLog("The custom touchpoint action has been executed");
	}
}
