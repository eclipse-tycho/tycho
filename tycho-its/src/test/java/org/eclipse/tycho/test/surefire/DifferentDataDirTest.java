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

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class DifferentDataDirTest extends AbstractTychoIntegrationTest {
    @Test
    public void test() throws Exception {
        Verifier verifier = getVerifier("surefire.dataDir");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();

        File baseDir = new File(verifier.getBasedir(), "target/data-dir");

        assertTrue("BaseDir " + baseDir.toString() + " doesnt exist", baseDir.exists());
        assertTrue("BaseDir " + baseDir.toString() + " is not a directory", baseDir.isDirectory());

        File metaData = new File(baseDir, ".metadata");
        assertTrue("Metadata doesnt exist", metaData.exists());
        assertTrue("Metadata is not a directory", metaData.isDirectory());
        FileUtils.deleteDirectory(baseDir);
    }
}
