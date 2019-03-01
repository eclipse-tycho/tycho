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

import java.util.Properties;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class RequireBundleTest extends AbstractTychoIntegrationTest {

    // requested in bug 485926
    @Test
    public void loadResourceFromRequireBundle() throws Exception {
        Verifier verifier = getVerifier("/surefire.requireBundle", false, true);
        Properties props = verifier.getSystemProperties();
        props.setProperty("oxygen-repo", P2Repositories.ECLIPSE_OXYGEN.toString());
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
    }

}
