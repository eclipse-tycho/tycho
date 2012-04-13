/*******************************************************************************
 * Copyright (c) 2012 Netcetera and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Michael Pellaton (Netcetera) - initial API and implementation
 *    Jan Sievers (SAP AG) - strip down integration test
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository.finalName;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

public class RepositoryFinalNameTest extends AbstractTychoIntegrationTest {

    private static final String FINAL_NAME = "testrepo-myqualifier";

    @Test
    public void testCustomFinalName() throws Exception {
        Verifier verifier = getVerifier("/p2Repository.finalName", false);
        verifier.getCliOptions().add("-Dp2.repo=" + ResourceUtil.P2Repositories.ECLIPSE_342.toString());
        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();
        File repositoryArchive = new File(verifier.getBasedir(), "target/" + FINAL_NAME + ".zip");
        String name = repositoryArchive.getAbsolutePath();
        assertTrue("Repository archive with name '" + name + "' does not exist.", repositoryArchive.isFile());
    }
}
