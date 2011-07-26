/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.featurePatch.build;

import static org.eclipse.tycho.test.util.ResourceUtil.P2Repositories.ECLIPSE_342;
import static org.eclipse.tycho.test.util.ResourceUtil.P2Repositories.ECLIPSE_352;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class FeaturePatchTest extends AbstractTychoIntegrationTest {

    @Test
    public void testFeaturePatch() throws Exception {
        Verifier verifier = getVerifier("featurePatch.build", false);

        verifier.getSystemProperties().setProperty("e342-url", ECLIPSE_342.toString());
        verifier.getSystemProperties().setProperty("e352-url", ECLIPSE_352.toString());

        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
    }
}
