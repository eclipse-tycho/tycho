/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.p2Inf.extraUnits;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

public class ExtraUnitsTest extends AbstractTychoIntegrationTest {

    @Test
    public void testExtraUnitsDontSpoilDependencyArtifacts() throws Exception {
        Verifier verifier = getVerifier("/p2Inf.extraUnits", false);
        verifier.getCliOptions().add("-Dp2.repo=" + ResourceUtil.P2Repositories.ECLIPSE_342);
        verifier.executeGoal("verify");

        /*
         * With bug 375715, the ID of the feature in the resolved project dependencies was set wrong
         * (leading to "peu.feature is not part of the project build target platform): The ID of one
         * of the extra IUs was used instead of the feature ID.
         */
        verifier.verifyErrorFreeLog();
    }
}
