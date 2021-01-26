/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
        props.setProperty("oxygen-repo", P2Repositories.ECLIPSE_LATEST.toString());
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
    }

}
