/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.mngeclipse1105PackageRootFiles;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class PackageRootFilesTest extends AbstractTychoIntegrationTest {

    @Override
    protected Verifier getVerifier(String test) throws Exception {
        Verifier verifier = super.getVerifier(test);

        // lock build environment to something specific
        verifier.getSystemProperties().setProperty("osgi.os", "macosx");
        verifier.getSystemProperties().setProperty("osgi.ws", "cocoa");
        verifier.getSystemProperties().setProperty("osgi.arch", "x86_64");

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
