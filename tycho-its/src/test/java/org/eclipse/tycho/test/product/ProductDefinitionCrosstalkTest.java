/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

// TODO make this a unit test
public class ProductDefinitionCrosstalkTest extends AbstractTychoIntegrationTest {

	private static final String BUNDLE_ONLY_IN_PRODUCT_A = "org.eclipse.equinox.registry";
	private static final String BUNDLE_ONLY_IN_PRODUCT_B = "org.eclipse.core.jobs";

	@Test
	public void testProductContentNotLeakedBetweenProducts() throws Exception {
		// an eclipse-repository module with two product definitions, each containing
		// exactly one bundle
		Verifier verifier = getVerifier("product.crosstalk", false);
		verifier.addCliArgument("-Dtest-data-repo=" + P2Repositories.ECLIPSE_342.toString());
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();

		File projectRoot = new File(verifier.getBasedir());
		P2RepositoryTool repository = P2RepositoryTool.forEclipseRepositoryModule(projectRoot);

		// this was bug 346532: one of the product IUs also required the bundle only
		// contained in the other product
		P2RepositoryTool.IU productA = repository.getUniqueIU("product-a");
		assertThat(productA.getRequiredIds(), hasItem(BUNDLE_ONLY_IN_PRODUCT_A));
		assertThat(productA.getRequiredIds(), not(hasItem(BUNDLE_ONLY_IN_PRODUCT_B)));

		P2RepositoryTool.IU productB = repository.getUniqueIU("product-b");
		assertThat(productB.getRequiredIds(), not(hasItem(BUNDLE_ONLY_IN_PRODUCT_A)));
		assertThat(productB.getRequiredIds(), hasItem(BUNDLE_ONLY_IN_PRODUCT_B));
	}
}
