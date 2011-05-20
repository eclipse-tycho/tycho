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
package org.eclipse.tycho.test.TYCHO0380twoJunitVersions;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class TwoJunitVersionsTest extends AbstractTychoIntegrationTest {
    @Test
    public void test() throws Exception {
        String targetPlatform = new File("repositories/junit4").getCanonicalPath();

        Verifier verifier = getVerifier("/TYCHO0380twoJunitVersions", false);
        verifier.getSystemProperties()
                .setProperty("tycho.targetPlatform", targetPlatform.replace('\\', '/').toString());
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();

        assertFileExists(new File(verifier.getBasedir()), "target/surefire-reports/some.Test.txt");
    }
}
