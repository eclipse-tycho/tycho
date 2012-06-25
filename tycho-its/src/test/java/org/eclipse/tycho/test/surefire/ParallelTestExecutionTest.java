/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.test.surefire;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class ParallelTestExecutionTest extends AbstractTychoIntegrationTest {

    @Test
    public void testParallelExecution() throws Exception {
        Verifier verifier = getVerifier("/surefire.junit47");
        verifier.getSystemProperties().setProperty("parallel", "classes");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("Concurrency config is parallel='classes', perCoreThreadCount=true, threadCount=3, useUnlimitedThreads=false");
    }

}
