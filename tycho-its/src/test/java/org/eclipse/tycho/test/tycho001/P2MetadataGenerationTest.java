/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.tycho001;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class P2MetadataGenerationTest extends AbstractTychoIntegrationTest {

	@Test
	public void test() throws Exception {
		Verifier verifier = getVerifier("tycho001");

		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		File site = new File(verifier.getBasedir(), "site/target/repository");
		assertTrue(new File(site, "artifacts.jar").canRead());
		assertTrue(new File(site, "content.jar").canRead());

	}

}
