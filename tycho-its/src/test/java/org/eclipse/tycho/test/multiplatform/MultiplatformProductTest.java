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
package org.eclipse.tycho.test.multiplatform;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class MultiplatformProductTest extends AbstractTychoIntegrationTest {
    @Test
    public void exportProduct() throws Exception {
        Verifier verifier = getVerifier("multiPlatform.product");
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();

        File targetdir = new File(verifier.getBasedir(), "tycho.demo.rcp/target");

        // assert expanded product folders have proper swt fragments
        assertFileExists(targetdir,
                "products/tychodemo/linux.gtk.x86_64/eclipse/plugins/org.eclipse.swt.gtk.linux.x86_64_*.jar");
        assertFileExists(targetdir,
                "products/tychodemo/macosx.cocoa.x86_64/eclipse/plugins/org.eclipse.swt.cocoa.macosx.x86_64_*.jar");
        assertFileExists(targetdir,
                "products/tychodemo/win32.win32.x86_64/eclipse/plugins/org.eclipse.swt.win32.win32.x86_64_*.jar");

        // assert native launchers
        Assert.assertTrue(new File(targetdir, "products/tychodemo/linux.gtk.x86_64/eclipse/tychodemo").canRead());
        // TODO osx
        Assert.assertTrue(new File(targetdir, "products/tychodemo/win32.win32.x86_64/eclipse/tychodemo.exe").canRead());

        // assert product zip was created for each target environment
        Assert.assertTrue(new File(targetdir, "tycho.demo.rcp-1.0.0-linux.gtk.x86_64.zip").canRead());
        Assert.assertTrue(new File(targetdir, "tycho.demo.rcp-1.0.0-macosx.cocoa.x86_64.zip").canRead());
        Assert.assertTrue(new File(targetdir, "tycho.demo.rcp-1.0.0-win32.win32.x86_64.zip").canRead());
    }

}
