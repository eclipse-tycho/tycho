/*******************************************************************************
 * Copyright (c) 2013 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mickael Istria (JBoss by Red Hat) - initial implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

public class PlatformSpecificDependencyTest extends AbstractTychoIntegrationTest {

    @Test
    public void testPlatformSpecificDependency() throws Exception {
        Verifier verifier = getVerifier("surefire.platformSpecificDependency", false); //$NON-NLS-1$
        List<String> options = verifier.getCliOptions();
        // Has to be a repo that contains some platform-specific artifacts
        options.add("-Dp2.repo.url=" + ResourceUtil.P2Repositories.ECLIPSE_342.toString()); //$NON-NLS-1$
        options.add("-DcurrentPlatformSuffix=" + getCurrentPlatformSuffix()); //$NON-NLS-1$
        verifier.executeGoal("integration-test"); //$NON-NLS-1$
        verifier.verifyErrorFreeLog();
    }

    private static String getCurrentPlatformSuffix() {
        TargetEnvironment environment = TargetEnvironment.getRunningEnvironment();
        StringBuilder res = new StringBuilder();
        res.append(environment.getWs());
        res.append("."); //$NON-NLS-1$
        res.append(environment.getOs());
        if (!environment.getOs().equals("macosx")) { //$NON-NLS-1$
            res.append("."); //$NON-NLS-1$
            res.append(environment.getArch());
        }
        return res.toString();
    }

}
