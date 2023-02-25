/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.TYCHO418pomDependencyConsider;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class PomDependencyConsiderTest extends AbstractTychoIntegrationTest {
	@Test
	public void testPomDependenciesConsider() throws Exception {
		Verifier verifier = getVerifier("/TYCHO418pomDependencyConsider/artifact");
		verifier.executeGoal("install");
		verifier.verifyErrorFreeLog();

		verifier = getVerifier("/TYCHO418pomDependencyConsider", false);
		verifier.executeGoal("install");
		verifier.verifyErrorFreeLog();

		File basedir = new File(verifier.getBasedir());

		assertTrue(new File(basedir,
				"site/target/repository/plugins/TYCHO418pomDependencyConsider.artifact_0.0.1.SNAPSHOT.jar").canRead());
	}

}
