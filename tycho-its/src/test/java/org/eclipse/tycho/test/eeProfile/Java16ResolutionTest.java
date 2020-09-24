/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.junit.BeforeClass;
import org.junit.Test;

public class Java16ResolutionTest extends AbstractTychoIntegrationTest {

    private static File buildResult;

    @BeforeClass
    public static void setUp() throws Exception {
        buildResult = new Java16ResolutionTest().runBuild();
    }

    public File runBuild() throws Exception {
        Verifier verifier = getVerifier("eeProfile.java16", false);
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
        return new File(verifier.getBasedir());
    }

    @Test
    public void testProductBuildForJava16() throws Exception {
        // a p2 repository that contains a product for Java 16
        P2RepositoryTool productRepo = P2RepositoryTool.forEclipseRepositoryModule(new File(buildResult, "repository"));
        List<String> jreUnitVersions = productRepo.getUnitVersions("a.jre.javase");
        // we expect java 16
        assertThat(jreUnitVersions, hasItem("16.0.0"));
    }

}
