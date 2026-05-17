/*******************************************************************************
 * Copyright (c) 2008, 2018 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.resolver;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

// tests that optional dependencies are put on the compile class path (bug 351842)
public class OptionalDependenciesTest extends AbstractTychoIntegrationTest {

	@Test
	public void testOptionallyRequiredBundleIsOnCompileClassPath() throws Exception {
		Verifier verifier = getVerifier("/resolver.optionalDependencies/require-bundle", true);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testOptionallyRequiredBundleCanBeIgnored() throws Exception {
		Verifier verifier = getVerifier("/resolver.optionalDependencies/require-bundle-ignore", false);
		// empty target platform -> dependency would not resolve if the project had not
		// overridden the optionalDependencies=require default
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	}

}
