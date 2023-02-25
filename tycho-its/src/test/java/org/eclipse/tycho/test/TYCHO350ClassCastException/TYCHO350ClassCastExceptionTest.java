/*******************************************************************************
 * Copyright (c) 2022 Kichwa Coders Canada Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.TYCHO350ClassCastException;

import static java.util.Arrays.asList;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class TYCHO350ClassCastExceptionTest extends AbstractTychoIntegrationTest {
	@Test
	public void test() throws Exception {
		Verifier verifier = getVerifier("/TYCHO350ClassCastException", false);
		verifier.executeGoals(asList("verify"));
		verifier.verifyErrorFreeLog();
	}

}
