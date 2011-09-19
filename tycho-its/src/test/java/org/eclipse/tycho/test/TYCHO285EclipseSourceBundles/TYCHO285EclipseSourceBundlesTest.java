/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.TYCHO285EclipseSourceBundles;

import java.io.File;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import junit.framework.Assert;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class TYCHO285EclipseSourceBundlesTest extends AbstractTychoIntegrationTest {
    @Test
    public void testEclipseSourceBundleManifestAttributes() throws Exception {
        Verifier verifier = getVerifier("/TYCHO285EclipseSourceBundles");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();

        File sourceJarFile = new File(verifier.getBasedir(), "target/bundle-1.2.3-SNAPSHOT-sources.jar");
        Assert.assertTrue(sourceJarFile.exists());

        Manifest manifest = new JarFile(sourceJarFile).getManifest();
        Assert.assertNotNull(manifest);
        Attributes mainAttributes = manifest.getMainAttributes();

        Assert.assertEquals("bundle.source", mainAttributes.getValue("Bundle-SymbolicName"));
        Assert.assertEquals("1.2.3.TAGNAME", mainAttributes.getValue("Bundle-Version"));
        Assert.assertEquals("bundle;version=\"1.2.3.TAGNAME\";roots:=\".\"",
                mainAttributes.getValue("Eclipse-SourceBundle"));
    }

}
