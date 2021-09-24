/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
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
import org.junit.Test;

public class ResolverTests extends AbstractTychoIntegrationTest {

	/**
	 * This test case tests a combination that at a first glance looks very simple
	 * but is hard to resolve due to the structure of 'org.eclipse.core.runtime'
	 * bundle. One can force a failure by running the following commandline
	 * 
	 * <pre>
	 * mvn clean install -Dtycho.equinox.resolver.batch.size=1 -Dtycho.equinox.resolver.uses=true
	 * </pre>
	 * 
	 * what then fails with: Bundle was not resolved because of a uses constraint
	 * violation, so this test effectively ensures that the defaults are working
	 * without any special options
	 * 
	 * @throws Exception if anything goes wrong
	 */
	@Test
	public void testUsesConstraintViolations() throws Exception {

		Verifier verifier = getVerifier("/usesConstraintViolations");
		verifier.executeGoal("compile");
		verifier.verifyErrorFreeLog();
	}

}
