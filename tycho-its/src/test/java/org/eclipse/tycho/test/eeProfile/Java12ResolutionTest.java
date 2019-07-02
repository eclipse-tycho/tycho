/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.eeProfile;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.junit.BeforeClass;
import org.junit.Test;

public class Java12ResolutionTest extends AbstractTychoIntegrationTest {

    private static File buildResult;

    @BeforeClass
    public static void setUp() throws Exception {
        buildResult = new Java12ResolutionTest().runBuild();
    }

    public File runBuild() throws Exception {
        Verifier verifier = getVerifier("eeProfile.java12", false);
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
        return new File(verifier.getBasedir());
    }

    @Test
    public void testProductBuildForJava12() throws Exception {
        // a p2 repository that contains a product for Java 12
        P2RepositoryTool productRepo = P2RepositoryTool.forEclipseRepositoryModule(new File(buildResult, "repository"));
        List<String> jreUnitVersions = productRepo.getUnitVersions("a.jre.javase");
     // we expect java 12 (java 10 provides more system packages) 
        assertThat(jreUnitVersions, hasItem("12.0.0"));
    }

}
