/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.TYCHO449SrcIncludesExcludes;

import java.io.File;
import java.util.jar.JarFile;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class Tycho449SrcIncludesExcludesTest extends AbstractTychoIntegrationTest {

    @Test
    public void testDefaultSourceBundleSuffix() throws Exception {
        Verifier verifier = getVerifier("/TYCHO449SrcIncludesExcludes", false);
        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();
        JarFile sourceJar = new JarFile(new File(verifier.getBasedir(),
                "target/TestSourceIncludesExcludes-1.0.0-SNAPSHOT-sources.jar"));
        try {
            Assert.assertNull(sourceJar.getEntry("resourceFolder/.hidden/toBeExcluded.txt"));
            Assert.assertNull(sourceJar.getEntry("resourceFolder/.svn/"));
            Assert.assertNotNull(sourceJar.getEntry("resourceFolder/test.txt"));
            Assert.assertNotNull(sourceJar.getEntry("resource.txt"));
            Assert.assertNotNull(sourceJar.getEntry("additionalResource.txt"));
        } finally {
            sourceJar.close();
        }
    }

}
