/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.pomDependencyConsider.p2Data.generate;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.jar.JarFile;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.junit.Assert;
import org.junit.Test;

public class PomDependencyOnNonTychoArtifactTest extends AbstractTychoIntegrationTest {
    private static final String POM_DEPENDENCY_BUNDLE_ID = "com.google.gson";
    private static final String POM_DEPENDENCY_BUNDLE_VERSION = "1.6.0";
    private static final String POM_DEPENDENCY_CLASSIFIER_BUNDLE_ID = "org.eclipse.jdt.compiler.apt.source";
    private static final String POM_DEPENDENCY_CLASSIFIER_BUNDLE_VERSION = "1.0.500.v20120423-0553";

    @Test
    public void testP2DataGeneratedForPomDependency() throws Exception {
        // project with a POM dependency on a bundle not built by Tycho
        Verifier verifier = getVerifier("pomDependencyConsider.p2Data.generate", false);

        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();

        String testProjectRoot = verifier.getBasedir();
        P2RepositoryTool p2Repo = P2RepositoryTool.forEclipseRepositoryModule(new File(testProjectRoot, "repository"));

        // this was bug TYCHO-570: the build passed, but the POM dependency bundle was missing
        File expectedBundle = p2Repo.getBundleArtifact(POM_DEPENDENCY_BUNDLE_ID, POM_DEPENDENCY_BUNDLE_VERSION);
        Assert.assertTrue(expectedBundle.isFile());
        // bug 368596 assert that pom dependency with classifier works
        File sourceBundle = p2Repo.getBundleArtifact(POM_DEPENDENCY_CLASSIFIER_BUNDLE_ID,
                POM_DEPENDENCY_CLASSIFIER_BUNDLE_VERSION);
        Assert.assertTrue(sourceBundle.isFile());
        JarFile jarFile = new JarFile(sourceBundle);
        try {
            String sourceBundleSymbolicName = jarFile.getManifest().getMainAttributes().getValue("Bundle-SymbolicName");
            assertEquals(POM_DEPENDENCY_CLASSIFIER_BUNDLE_ID, sourceBundleSymbolicName);
        } finally {
            jarFile.close();
        }
    }
}
