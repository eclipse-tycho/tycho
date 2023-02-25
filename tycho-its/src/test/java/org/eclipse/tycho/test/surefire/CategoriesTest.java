/*******************************************************************************
 * Copyright (c) 2016, 2021 SAP SE and others.
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
package org.eclipse.tycho.test.surefire;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class CategoriesTest extends AbstractTychoIntegrationTest {

	@Test
	public void testIncludeExcludeCategories() throws Exception {
		Verifier verifier = getVerifier("/surefire.junit47/categories", false);
		verifier.addCliArgument("-Dkepler-repo=" + P2Repositories.ECLIPSE_LATEST.toString());
		verifier.addCliArgument("-Dgroups=tycho.demo.itp01.tests.FastTests");
		verifier.addCliArgument("-DexcludedGroups=tycho.demo.itp01.tests.SlowTests");
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	}

}
