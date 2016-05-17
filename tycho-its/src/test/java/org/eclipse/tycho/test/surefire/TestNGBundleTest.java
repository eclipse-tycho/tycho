/*******************************************************************************
 * Copyright (c) 2016 Bachmann electronic GmbH. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bachmann electronic GmbH. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import static org.eclipse.tycho.test.util.SurefireUtil.testResultFile;
import static org.eclipse.tycho.test.util.TychoMatchers.exists;
import static org.junit.Assert.assertThat;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class TestNGBundleTest extends AbstractTychoIntegrationTest {

    @Test
    public void test() throws Exception {

        Verifier verifier = getVerifier("surefire.testng");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();

        assertThat(testResultFile(verifier.getBasedir(), "bundle.test", "TestNGTest"), exists());

    }

}
