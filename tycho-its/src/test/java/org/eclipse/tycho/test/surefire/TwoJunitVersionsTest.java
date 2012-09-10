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

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

// regression test for TYCHO-380
// TODO is this case really tested? The target platform only a single JUnit version!
public class TwoJunitVersionsTest extends AbstractTychoIntegrationTest {
    @Test
    public void test() throws Exception {
        String targetPlatform = new File("repositories/junit4").getCanonicalPath();

        Verifier verifier = getVerifier("surefire.twoJunitVersions", false);
        verifier.getSystemProperties()
                .setProperty("tycho.targetPlatform", targetPlatform.replace('\\', '/').toString());
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();

        assertFileExists(new File(verifier.getBasedir()), "target/surefire-reports/some.Test.txt");
    }
}
