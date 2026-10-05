/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.resolver;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Nested jars of the Bundle-ClassPath are injected as system-scoped Maven dependencies, both for
 * reactor bundles (rnj.provider) and for bundles from the target platform (org.apache.ant). With
 * Maven 3.10 (Maven Resolver 2.x), this failed with "Invalid Collect Request" because the
 * Bundle-ClassPath entry was used as classifier, see
 * <a href="https://github.com/eclipse-tycho/tycho/issues/6399">#6399</a>.
 */
public class NestedJarsTest extends AbstractTychoIntegrationTest {

	@Test
	public void testNestedJars() throws Exception {
		Verifier verifier = getVerifier("resolver.nestedJars", true);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	}

}
