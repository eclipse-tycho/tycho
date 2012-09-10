/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import java.util.Arrays;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class DirectTestPluginInvocationTest extends AbstractTychoIntegrationTest {

    @Test
    public void test() throws Exception {
        // a project with an eclipse-plugin and an eclipse-test-plugin module
        Verifier verifier = getVerifier("surefire.cli"); // TODO simplify test project; is currently a copy of the "tycho98" project

        // calling the test plugin on the aggregator failed with an NPE -> this was MNGECLIPSE-999
        verifier.executeGoals(Arrays.asList(new String[] { "package", "org.eclipse.tycho:tycho-surefire-plugin:test" }));
        verifier.verifyErrorFreeLog();
    }

}
