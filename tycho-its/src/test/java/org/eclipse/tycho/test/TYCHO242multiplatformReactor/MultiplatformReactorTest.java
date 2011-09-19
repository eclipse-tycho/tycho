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
package org.eclipse.tycho.test.TYCHO242multiplatformReactor;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class MultiplatformReactorTest extends AbstractTychoIntegrationTest {

    @Test
    public void testMultiplatformReactorBuild() throws Exception {
        Verifier verifier = getVerifier("/TYCHO242multiplatformReactor", false);
        verifier.getCliOptions().add("-DtestRepository=" + new File("repositories/e342").getCanonicalFile().toURI());
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();

        // assert product got proper platform fragments 
        File productTarget = new File(verifier.getBasedir(), "product/target");
        assertFileExists(productTarget, "linux.gtk.x86_64/eclipse/plugins/fragment.linux_0.0.1.*.jar");
        assertFileExists(productTarget, "win32.win32.x86/eclipse/plugins/fragment.windows_0.0.1.*.jar");

        // assert site got all platform fragments
        File siteproductTarget = new File(verifier.getBasedir(), "site/target");
        assertFileExists(siteproductTarget, "site/plugins/fragment.linux_0.0.1.*.jar");
        assertFileExists(siteproductTarget, "site/plugins/fragment.windows_0.0.1.*.jar");
    }
}
