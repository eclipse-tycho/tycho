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
package org.eclipse.tycho.test.mngeclipse1031;

import static org.eclipse.tycho.test.util.EnvironmentUtil.isEclipse32Platform;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class Junit4TestBundleTest extends AbstractTychoIntegrationTest {

    @Test
    public void test() throws Exception {

        if (isEclipse32Platform()) {
            // there is no junit4 support in eclipse 3.2
            return;
        }

        Verifier verifier = getVerifier("MNGECLIPSE1031/bundle.test");

        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();

        File testReport = new File(verifier.getBasedir(), "target/surefire-reports/TEST-bundle.test.BundleTest.xml");
        Assert.assertTrue(testReport.exists());
    }

}
