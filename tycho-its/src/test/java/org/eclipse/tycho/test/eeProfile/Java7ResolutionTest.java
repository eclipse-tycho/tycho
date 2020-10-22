/*******************************************************************************
 * Copyright (c) 2012, 2019 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.eeProfile;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.junit.Assume;
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
        P2RepositoryTool bundleOnlyRepo = P2RepositoryTool
                .forEclipseRepositoryModule(new File(buildResult, "repository1"));

        /*
         * With bug 384494, there was no matching export to the package import of the bundle in the
         * repository (despite includeAllDependencies=true) and hence a p2 client may have been
         * unable to install the bundle from the repository.
         */
        List<String> availablePackages = bundleOnlyRepo.getAllProvidedPackages();
        assertThat(availablePackages, hasItem("java.nio.file"));
    }

    @Test
    public void testProductBuildForJava7() throws Exception {
        Assume.assumeTrue(System.getProperty("java.specification.version").compareTo("11") < 0);
        // a p2 repository that contains a product for Java 7
        P2RepositoryTool productRepo = P2RepositoryTool
                .forEclipseRepositoryModule(new File(buildResult, "repository2"));

        // with bug 384494, the "a.jre" IU required by the product was missing in the p2 repository
        List<String> jreUnitVersions = productRepo.getUnitVersions("a.jre.javase");
        assertThat(jreUnitVersions, hasItem("1.7.0"));
    }

    @Test
    public void testP2ResolutionWithLowerBREEThanRequiredBundle() throws Exception {
        Verifier verifier = getVerifier("eeProfile.java7/bundle2", false);
        verifier.getCliOptions().add("-Dp2.repo.url=" + new File(buildResult, "repository1/target/repository").toURI());
        verifier.executeGoal("verify");

        // with bug 434959, p2 resolver would fail
        verifier.verifyErrorFreeLog();
    }

}
