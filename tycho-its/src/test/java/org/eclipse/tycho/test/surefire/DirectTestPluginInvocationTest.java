/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
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

import java.util.Arrays;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class DirectTestPluginInvocationTest extends AbstractTychoIntegrationTest {

    @Test
    public void test() throws Exception {
        // a project with an eclipse-plugin and an eclipse-test-plugin module
        Verifier verifier = getVerifier("surefire.cli"); // TODO simplify test project; is currently a copy of the "tycho98" project

        // calling the test plugin on the aggregator failed with an NPE -> this was MNGECLIPSE-999
        verifier.executeGoals(Arrays.asList("package", "org.eclipse.tycho:tycho-surefire-plugin:test"));
        verifier.verifyErrorFreeLog();
    }

}
