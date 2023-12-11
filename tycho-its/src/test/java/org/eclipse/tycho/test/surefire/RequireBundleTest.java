/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.surefire;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class RequireBundleTest extends AbstractTychoIntegrationTest {

	// requested in bug 485926
	@Test
	public void loadResourceFromRequireBundle() throws Exception {
		Verifier verifier = getVerifier("/surefire.requireBundle", true, true);
		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void requireMultipleVersionsOfABundle() throws Exception {
		Verifier verifier = getVerifier("/surefire.requireBundle.multipleVersions", true, true);
		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();
	}

}
