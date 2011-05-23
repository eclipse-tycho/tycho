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
package org.eclipse.tycho.test;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.junit.Assert;
import org.junit.Test;

public class MNGECLIPSE902Test extends AbstractTychoIntegrationTest {

    @Test
    public void test() throws Exception {
        Verifier verifier = getVerifier("MNGECLIPSE902");

        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();

        File testReport = new File(verifier.getBasedir(), "p1.test/target/surefire-reports/TEST-p1.test.ATest.xml");
        Assert.assertTrue(testReport.exists());
    }
}
