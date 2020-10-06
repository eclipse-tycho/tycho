/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tycho.test.pomDependencyConsider;

import java.io.File;

import org.apache.maven.it.Verifier;
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

        Assert.assertTrue(p2Repo.getAllUnitIds().stream().anyMatch("org.test"::equals));
        Assert.assertTrue(p2Repo.getAllUnitIds().stream().anyMatch(id -> id.contains("undertow")));
        Assert.assertTrue(p2Repo.getAllProvidedPackages().stream()
                .anyMatch(packageName -> packageName.contains("org.netbeans.api.annotations.common")));
    }
}
