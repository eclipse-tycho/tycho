/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.bug368079_productArchiveFormat;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

public class ProductArchiveFormatTest extends AbstractTychoIntegrationTest {
    @Test
    public void test() throws Exception {
        Verifier verifier = getVerifier("/368079_productArchiveFormat", false);
        verifier.getCliOptions().add("-De342-repo=" + ResourceUtil.P2Repositories.ECLIPSE_342.toString());
        verifier.executeGoals(Arrays.asList("clean", "install"));
        verifier.verifyErrorFreeLog();

        File basedir = new File(verifier.getBasedir());

        assertFileExists(basedir, "target/products/customArchiveName-win32.win32.x86.zip");
        assertFileExists(basedir, "target/products/customArchiveName-linux.gtk.x86.zip");
        assertFileExists(basedir, "target/products/customArchiveName-macosx.cocoa.x86_64.tar.gz");
    }

}
