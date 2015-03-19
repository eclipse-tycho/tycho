/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.pomless;

import static java.util.Arrays.asList;
import static org.eclipse.tycho.extras.its.TychoMatchers.isFile;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.extras.its.AbstractTychoExtrasIntegrationTest;
import org.junit.Test;

public class TychoPomlessITest extends AbstractTychoExtrasIntegrationTest {

    @Test
    public void testPomlessBuildExtension() throws Exception {
        Verifier verifier = getVerifier("testpomless", false);
        verifier.addCliOption("-Dp2.repo=" + new File("repositories/kepler").getAbsoluteFile().toURI().toString());
        verifier.executeGoals(asList("clean", "verify"));
        verifier.verifyErrorFreeLog();
        // sanity check pom-less if bundle, test bundle and feature have been built
        File baseDir = new File(verifier.getBasedir());
        assertThat(new File(baseDir, "bundle1/target/pomless.bundle-0.1.0-SNAPSHOT.jar"), isFile());
        assertThat(new File(baseDir, "bundle1.tests/target/pomless.bundle.tests-1.0.1.jar"), isFile());
        assertThat(new File(baseDir, "feature/target/pomless.feature-1.0.0-SNAPSHOT.jar"), isFile());
    }

}
