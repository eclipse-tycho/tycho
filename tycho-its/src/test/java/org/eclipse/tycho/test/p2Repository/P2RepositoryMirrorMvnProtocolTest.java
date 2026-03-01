/*******************************************************************************
 * Copyright (c) 2025 Ed Merks and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ed Merks - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import java.util.List;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class P2RepositoryMirrorMvnProtocolTest extends AbstractTychoIntegrationTest {

	/**
	 * Tests whether Tycho is able to recover from a bad mirror repository. If
	 * multiple mirrors are specified for a repository, Tycho might be able to
	 * continue by requesting an artifact from a different mirror, depending on the
	 * error code returned by Equinox.
	 */
	@Test
	public void testMirrorMvnProtocol() throws Exception {
		Verifier verifier = getVerifier("p2Repository.mirror.mvn.protocol", false);
		verifier.executeGoals(List.of("verify"));
		verifier.verifyErrorFreeLog();
	}
}
