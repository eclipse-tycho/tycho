/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.extras.pde;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.extras.its.AbstractTychoExtrasIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class TestListDependencies extends AbstractTychoExtrasIntegrationTest {

    @Test
    public void testDependencyInReactor() throws Exception {
        Verifier verifier = getVerifier("dependencyList/multi-modules", false);
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
        File file = new File(verifier.getBasedir(), "dependent/target/dependencies-list.txt");
        Assert.assertTrue(file.exists());
        try (BufferedReader reader = Files.newBufferedReader(file.toPath());) {
            File dependency = new File(reader.readLine());
            Assert.assertTrue(dependency.exists());
            Assert.assertEquals("dependency-0.1.0-SNAPSHOT.jar", dependency.getName());
        }
    }

    @Test
    public void testDependencyWithNestedJar() throws Exception {
        Verifier verifier = getVerifier("dependencyList/dependency-with-nested-jar", false);
        verifier.addCliArgument("-Dp2-repo=" + P2_REPO);
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
        File file = new File(verifier.getBasedir(), "target/dependencies-list.txt");
        Assert.assertTrue(file.exists());
        try (BufferedReader reader = Files.newBufferedReader(file.toPath()) //
        ) { //
            File dependency = new File(reader.readLine());
            Assert.assertTrue(dependency.exists());
            Assert.assertEquals("org.junit-4.12.0.v201504281640.jar", dependency.getName());
        }
    }
}
