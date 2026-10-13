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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.P2RepositoryTool.IU;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.jupiter.api.Test;

// TODO make this a unit test?
public class BuildProductWithIgnoredContentTest extends AbstractTychoIntegrationTest {

	private static final String BUNDLE_IN_PRODUCT_FILE = "org.example.toBeIgnored";
	private static final String FEATURE_IN_PRODUCT_FILE = "org.eclipse.equinox.executable.feature.group";

	@Test
	public void testBuildOfProductWithBundlesDespiteUseFeaturesTrue() throws Exception {
		/*
		 * Project with a product file which lists a feature, although the useFeatures
		 * attribute is false. The current (Indigo) product editor produces such a file
		 * when changing the mode in which the content is defined from features to
		 * bundles.
		 */
		Verifier verifier = getVerifier("product.sourceFile.leftovers", false);
		verifier.addCliOption("-Dtest-data-repo=" + P2Repositories.ECLIPSE_342.toString());
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();

		// check product IU
		P2RepositoryTool p2Repository = P2RepositoryTool.forEclipseRepositoryModule(new File(verifier.getBasedir()));
		IU product = p2Repository.getUniqueIU("psl.product");
		List<String> requiredIds = product.getRequiredIds();
		assertFalse(requiredIds.contains(BUNDLE_IN_PRODUCT_FILE), requiredIds.toString());
		assertTrue(requiredIds.contains(FEATURE_IN_PRODUCT_FILE), requiredIds.toString());

		// verify that IUs included in product exist
		List<String> inclusionIds = product.getInclusionIds();
		assertNotEquals(0, inclusionIds.size());
		List<String> allUnitIds = p2Repository.getAllUnitIds();
		assertTrue(allUnitIds.containsAll(inclusionIds), allUnitIds.toString());
	}
}
