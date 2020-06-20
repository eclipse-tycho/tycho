/*******************************************************************************
 * Copyright (c) 2020 Alexander Aumann and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Aumann - bug 564423
 *******************************************************************************/

package org.eclipse.tycho.test.compiler;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class JdtBreeJava8 extends AbstractTychoIntegrationTest {

    @Test
    public void testCompilerJdtBreeJava8() throws Exception {
        Verifier verifier = getVerifier("compiler.jdt.bree.java8", false);
        verifier.executeGoal("compile");
    }

}
