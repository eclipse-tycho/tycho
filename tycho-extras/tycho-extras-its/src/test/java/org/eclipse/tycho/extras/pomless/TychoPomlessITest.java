/*******************************************************************************
 * Copyright (c) 2015, 2019 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *     Christoph Läubrich - add testPomlessFlatBuildExtension
 *******************************************************************************/
package org.eclipse.tycho.extras.pomless;

import static java.util.Arrays.asList;
import static org.eclipse.tycho.extras.its.TychoMatchers.isFile;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.extras.its.AbstractTychoExtrasIntegrationTest;
import org.junit.Test;

public class TychoPomlessITest extends AbstractTychoExtrasIntegrationTest {

    @Test
    public void testPomlessBuildExtension() throws Exception {
        Verifier verifier = getVerifier("testpomless", false);
        verifier.executeGoals(asList("clean", "verify"));
        verifier.verifyErrorFreeLog();
        // sanity check pom-less if bundle, test bundle and feature have been built
        check(new File(verifier.getBasedir()));

    }

    private void check(File baseDir) {
        assertThat(new File(baseDir, "bundle1/target/pomless.bundle-0.1.0-SNAPSHOT.jar"), isFile());
        assertThat(new File(baseDir, "bundle1.tests/target/pomless.bundle.tests-1.0.1.jar"), isFile());
        assertThat(new File(baseDir, "feature/target/pomless.feature-1.0.0-SNAPSHOT.jar"), isFile());
        assertThat(new File(baseDir, "product/target/my.test.product.pomless-1.0.0.zip"), isFile());
        isRepository(baseDir, "product");
        assertThat(new File(baseDir, "site1/target/site1.eclipse-repository-0.0.1-SNAPSHOT.zip"), isFile());
        isRepository(baseDir, "site1");
    }

    @Test
    public void testPomlessFlatBuildExtension() throws Exception {
        Verifier verifier = getVerifier("testpomless-flat", false);
        verifier.addCliArguments("-f aggregate/pom.xml");
        verifier.executeGoals(asList("clean", "verify"));
        verifier.verifyErrorFreeLog();
        // sanity check pom-less if bundle, test bundle and feature have been built
        check(new File(verifier.getBasedir()));

    }

    @Test
    public void testPomlessStructuredBuildExtension() throws Exception {
        Verifier verifier = getVerifier("testpomless-structured", false);
        verifier.executeGoals(asList("clean", "verify"));
        verifier.verifyErrorFreeLog();
        // sanity check pom-less if bundle, test bundle and feature have been built
        File baseDir = new File(verifier.getBasedir());
        assertThat(new File(baseDir, "bundles/bundle1/target/pomless.bundle-0.1.0-SNAPSHOT.jar"), isFile());
        assertThat(new File(baseDir, "tests/bundle1.tests/target/pomless.bundle.tests-1.0.1.jar"), isFile());
        assertThat(new File(baseDir, "features/feature/target/pomless.feature-1.0.0-SNAPSHOT.jar"), isFile());
        assertThat(new File(baseDir, "releng/product/target/my.test.product.pomless-1.0.0.zip"), isFile());
        isRepository(baseDir, "releng/product");
        assertThat(new File(baseDir, "releng/site1/target/site1.eclipse-repository-0.0.1-SNAPSHOT.zip"), isFile());
        isRepository(baseDir, "releng/site1");

    }

    private void isRepository(File baseDir, String subdir) {
        assertThat(new File(baseDir, subdir + "/target/repository/artifacts.jar"), isFile());
        assertThat(new File(baseDir, subdir + "/target/repository/content.jar"), isFile());
    }

}
