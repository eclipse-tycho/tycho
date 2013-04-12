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

public class Java7ResolutionTest extends AbstractTychoIntegrationTest {

    private static File buildResult;

    @BeforeClass
    public static void setUp() throws Exception {
        buildResult = new Java7ResolutionTest().runBuild();
    }

    public File runBuild() throws Exception {
        Verifier verifier = getVerifier("eeProfile.java7", false);

        verifier.executeGoal("verify");

        // with bug 384494, the product could not be materialized
        verifier.verifyErrorFreeLog();

        return new File(verifier.getBasedir());
    }

    @Test
    public void testRepositoryAggregationForJava7() throws Exception {
        /*
         * A p2 repository that only contains the bundle importing the package javax.xml.ws.spi.http
         * (which is new in Java 7).
         */
        P2RepositoryTool bundleOnlyRepo = P2RepositoryTool.forEclipseRepositoryModule(new File(buildResult,
                "repository1"));

        /*
         * With bug 384494, there was no matching export to the package import of the bundle in the
         * repository (despite includeAllDependencies=true) and hence a p2 client may have been
         * unable to install the bundle from the repository.
         */
        List<String> availablePackages = bundleOnlyRepo.getAllProvidedPackages();
        assertThat(availablePackages, hasItem("javax.xml.ws.spi.http"));
    }

    @Test
    public void testProductBuildForJava7() throws Exception {
        // a p2 repository that contains a product for Java 7
        P2RepositoryTool productRepo = P2RepositoryTool
                .forEclipseRepositoryModule(new File(buildResult, "repository2"));

        // with bug 384494, the "a.jre" IU required by the product was missing in the p2 repository
        List<String> jreUnitVersions = productRepo.getUnitVersions("a.jre.javase");
        assertThat(jreUnitVersions, hasItem("1.7.0"));
    }
}
