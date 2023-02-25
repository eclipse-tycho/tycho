/*******************************************************************************
 * Copyright (c) 2012, 2021 SAP AG and others.
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

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class ProductDuplicateIUsTest extends AbstractTychoIntegrationTest {

	@Test
	public void testMultipleProductsNoDuplicateIUs() throws Exception {
		Verifier verifier = getVerifier("product.duplicateIUs", false);
		verifier.addCliArgument("-Dtest-data-repo=" + P2Repositories.ECLIPSE_LATEST.toString());
		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();
	}
}
