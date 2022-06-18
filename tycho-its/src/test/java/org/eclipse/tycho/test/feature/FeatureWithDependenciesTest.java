/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.feature;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class FeatureWithDependenciesTest extends AbstractTychoIntegrationTest {

	@Test
	public void testFeatureRestriction() throws Exception {
		Verifier verifier = getVerifier("feature.dependency", false, true);
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File pluginsFolder = new File(verifier.getBasedir(), "site/target/repository/plugins");
		assertTrue("No plugin folder created!", pluginsFolder.exists());
	}
}
