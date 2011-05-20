/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.mngeclipse1105PackageRootFiles;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class PackageRootFilesTest extends AbstractTychoIntegrationTest {

    @Override
    protected Verifier getVerifier(String test) throws Exception {
        Verifier verifier = super.getVerifier(test);

        verifier.getSystemProperties().setProperty("osgi.os", "macosx");
        verifier.getSystemProperties().setProperty("osgi.ws", "carbon");
        verifier.getSystemProperties().setProperty("osgi.arch", "x86");

        return verifier;
    }

    @Test
    public void test() throws Exception {
        Verifier verifier = getVerifier("MNGECLIPSE1105rootfiles");

        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();

        verifier.assertFilePresent("target/product/eclipse/jre/the_entire_jre.txt");
        verifier.assertFilePresent("target/product/eclipse/license.txt");

        verifier.assertFilePresent("target/product/eclipse/configuration/config.macosx.txt");
        verifier.assertFileNotPresent("target/product/eclipse/configuration/config.linux.txt");
        verifier.assertFileNotPresent("target/product/eclipse/configuration/config.win32.txt");
    }

}
