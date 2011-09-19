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
package org.eclipse.tycho.test.tycho32;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class TYCHO32Test extends AbstractTychoIntegrationTest {

    @Test
    public void test() throws Exception {
        Verifier verifier = getVerifier("TYCHO32");

        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();

        File testReport = new File(verifier.getBasedir(),
                "bundle.tests/target/surefire-reports/TEST-bundle.tests.SystemPropertyTest.xml");
        Assert.assertTrue(testReport.exists());
    }
}
