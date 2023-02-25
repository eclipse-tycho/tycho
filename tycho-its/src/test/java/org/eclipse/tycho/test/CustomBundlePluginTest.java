/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.junit.Test;

public class CustomBundlePluginTest extends AbstractTychoIntegrationTest {

	@Test
	public void testCustomBundleParent() throws Exception {
		Verifier verifier = getVerifier("custom-bundle-plugin/custom-bundle-parent", true, true);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		File attached = new File(verifier.getBasedir(),
				"custom.bundle.feature/target/site/plugins/custom.bundle.attached_1.0.0.123abc.jar");
		assertTrue("Missing expected file " + attached, attached.canRead());
	}

	@Test
	public void testUnresolvableCustomBundle() throws Exception {
		Verifier verifier = getVerifier("custom-bundle-plugin/unresolvable-custom-bundle", true, true);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		File attached = new File(verifier.getBasedir(),
				"target/unresolvable-custom-bundle-1.0.0-SNAPSHOT-attached.jar");
		assertTrue("Missing expected file " + attached, attached.canRead());
	}
}
