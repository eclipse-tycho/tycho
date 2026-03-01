/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.pomDependencies;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.junit.Assert;
import org.junit.Test;

public class PomDependencyWrapTest extends AbstractTychoIntegrationTest {

    @Test
    public void testWrap() throws Exception {
        // project with a POM dependency on a bundle not built by Tycho
        Verifier verifier = getVerifier("pomDependency.wrapAsBundle", false);

        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();

        String testProjectRoot = verifier.getBasedir();
        P2RepositoryTool p2Repo = P2RepositoryTool.forEclipseRepositoryModule(new File(testProjectRoot, "repository"));

        Assert.assertTrue(p2Repo.getAllUnitIds().stream().anyMatch("org.bundle"::equals));
        Assert.assertTrue(p2Repo.getAllUnitIds().stream().anyMatch(id -> id.contains("undertow")));
        Assert.assertTrue(
                p2Repo.getAllProvidedPackages().stream().anyMatch(packageName -> packageName.contains("io.undertow")));
    }
}
