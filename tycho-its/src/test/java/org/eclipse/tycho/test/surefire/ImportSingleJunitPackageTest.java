/*******************************************************************************
 * Copyright (c) 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

public class ImportSingleJunitPackageTest extends AbstractTychoIntegrationTest {

    @Test
    public void testImportSingleOrgJunitPackageTest() throws Exception {
        Verifier verifier = getVerifier("surefire.importSinglePackage", false);
        verifier.getCliOptions().add("-Dp2.repo=" + ResourceUtil.P2Repositories.ECLIPSE_KEPLER.toString());
        verifier.executeGoal("integration-test");
        // test for bug 369266
        verifier.verifyErrorFreeLog();
    }

}
