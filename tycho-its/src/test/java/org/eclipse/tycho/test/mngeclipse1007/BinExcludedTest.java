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
package org.eclipse.tycho.test.mngeclipse1007;

import java.io.File;
import java.util.zip.ZipFile;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class BinExcludedTest extends AbstractTychoIntegrationTest {

    @Test
    public void test() throws Exception {
        Verifier verifier = getVerifier("MNGECLIPSE1007");

        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();

        ZipFile zip = new ZipFile(new File(verifier.getBasedir(), "target/MNGECLIPSE1007-1.0.0.jar"));

        try {
            Assert.assertNotNull(zip.getEntry("files/included.txt"));
            Assert.assertNull(zip.getEntry("files/excluded.txt"));
        } finally {
            zip.close();
        }
    }

}
