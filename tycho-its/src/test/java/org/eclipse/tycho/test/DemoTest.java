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

import java.util.List;

import org.apache.maven.it.Verifier;
import org.junit.Test;

/**
 * This integration test builds and tests the demo projects we provide in the
 * repository
 */
public class DemoTest extends AbstractTychoIntegrationTest {

	@Test
	public void testSureFireDemo() throws Exception {
		runDemo("testing/surefire/", "-f", "with-maven-layout");
		runDemo("testing/surefire/", "-f", "with-source-folder");
	}

	@Test
	public void testTychoSureFireDemo() throws Exception {
		runDemo("testing/tycho/", "-f", "standalone");
		runDemo("testing/tycho/", "-f", "samemodule");
	}

	@Test
	public void testTychoBndDemo() throws Exception {
		runDemo("testing/bnd/", "-f", "osgi-test");
		// TODO add a TCK test demo, e.g. when h2 complies to the jdbc spec we can use
		// that as it is small and fast
	}

	protected Verifier runDemo(String test, String... xargs) throws Exception {
		Verifier verifier = super.getVerifier("../../demo/" + test, true, true);
		for (String xarg : xargs) {
			verifier.addCliOption(xarg);
		}
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
		return verifier;
	}
}
