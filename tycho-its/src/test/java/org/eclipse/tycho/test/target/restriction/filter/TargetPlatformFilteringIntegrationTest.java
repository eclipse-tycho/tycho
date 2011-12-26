/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.target.restriction.filter;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class TargetPlatformFilteringIntegrationTest extends AbstractTychoIntegrationTest {

    @Test
    public void test() throws Exception {
        Verifier verifier = getVerifier("target.restriction.filter", false);
        verifier.getSystemProperties().put("e342-repo", P2Repositories.ECLIPSE_342.toString());
        verifier.getSystemProperties().put("e352-repo", P2Repositories.ECLIPSE_352.toString());
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
    }
}
