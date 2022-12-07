/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.tycho476;

import java.io.File;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class ExecutionEnvironmentTest extends AbstractTychoIntegrationTest {

    @Test
    public void testCompilerSourceTargetConfigurationViaManifest() throws Exception {
        Verifier verifier = getVerifier("TYCHO476", false);
        verifier.executeGoal("compile");
        // compile only succeeds with source level 1.6 which
        // is configured indirectly via Bundle-RequiredExecutionEnvironment: JavaSE-1.6
        verifier.verifyErrorFreeLog();
        File classFile = new File(verifier.getBasedir(), "target/classes/TestRunnable.class");
        Assert.assertTrue(classFile.canRead());
        JavaClass javaClass = new ClassParser(classFile.getAbsolutePath()).parse();
        // bytecode major level 50 == target 1.6 
        Assert.assertEquals(50, javaClass.getMajor());
    }

}
