/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.eeProfile;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.junit.BeforeClass;
import org.junit.Test;

public class Java14ResolutionTest extends AbstractTychoIntegrationTest {

    private static File buildResult;

    @BeforeClass
    public static void setUp() throws Exception {
        buildResult = new Java14ResolutionTest().runBuild();
    }

    public File runBuild() throws Exception {
        Verifier verifier = getVerifier("eeProfile.java14", false);
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
        return new File(verifier.getBasedir());
    }

    @Test
    public void testProductBuildForJava14() throws Exception {
        // a p2 repository that contains a product for Java 14
        P2RepositoryTool productRepo = P2RepositoryTool.forEclipseRepositoryModule(new File(buildResult, "repository"));
        List<String> jreUnitVersions = productRepo.getUnitVersions("a.jre.javase");
        // we expect java 14 (java 10 provides more system packages) 
        assertThat(jreUnitVersions, hasItem("14.0.0"));
    }

}
