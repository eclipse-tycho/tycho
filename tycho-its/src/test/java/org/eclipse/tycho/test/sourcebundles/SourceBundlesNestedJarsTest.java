/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.test.sourcebundles;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.jar.JarFile;

import org.apache.maven.it.Verifier;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class SourceBundlesNestedJarsTest extends AbstractTychoIntegrationTest {

    @Test
    public void testDistinctSourceRoots() throws Exception {
        Verifier verifier = getVerifier("sourcebundle.nestedjars", false);
        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();
        File sourceJar = new File(verifier.getBasedir(), "target/test.distinct.sourceroots-1.0.0-sources.jar");
        assertTrue(sourceJar.isFile());
        JarFile jar = new JarFile(sourceJar);
        try {
            String sourceBundleHeader = jar.getManifest().getMainAttributes().getValue("Eclipse-SourceBundle");
            ManifestElement element = ManifestElement.parseHeader("", sourceBundleHeader)[0];
            String[] roots = element.getDirective("roots").split(",");
            assertEquals(new HashSet<String>(asList(".", "foosrc", "barsrc")), new HashSet<String>(asList(roots)));
            assertNotNull(jar.getEntry("Main.java"));
            assertNotNull(jar.getEntry("foosrc/Foo1.java"));
            assertNotNull(jar.getEntry("foosrc/Foo2.java"));
            assertNotNull(jar.getEntry("barsrc/Bar1.java"));
            assertNotNull(jar.getEntry("barsrc/Bar2.java"));
        } finally {
            jar.close();
        }
    }
}
