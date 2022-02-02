/*******************************************************************************
 * Copyright (c) 2021, 2022 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.xtend;

import static java.util.Arrays.asList;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class TychoXtendTest extends AbstractTychoIntegrationTest {

	@Test
	public void projectA() throws Exception {
		Verifier verifier = getVerifier("tycho.xtend");
		verifier.executeGoals(asList("clean", "install"));
		verifier.verifyErrorFreeLog();
	}
}
