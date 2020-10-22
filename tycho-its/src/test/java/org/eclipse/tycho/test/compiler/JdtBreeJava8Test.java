/*******************************************************************************
 * Copyright (c) 2020 Alexander Aumann and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Alexander Aumann - bug 564423
 *******************************************************************************/

package org.eclipse.tycho.test.compiler;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class JdtBreeJava8Test extends AbstractTychoIntegrationTest {

    @Test
    public void testCompilerJdtBreeJava8() throws Exception {
        Verifier verifier = getVerifier("compiler.jdt.bree.java8", false);
        verifier.executeGoal("compile");
    }

}
