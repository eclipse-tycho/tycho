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
package org.eclipse.tycho.test.MNGECLIPSE949jarDirectoryBundles;

import java.io.File;
import java.io.FileFilter;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.core.osgitools.DefaultBundleReader;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class JarDirectoryBundlesTest extends AbstractTychoIntegrationTest {

    @Test
    public void test() throws Exception {
        Verifier verifier = getVerifier("MNGECLIPSE949jarDirectoryBundles");

        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();

        File[] sitePlugins = new File(verifier.getBasedir(), "site/target/site/plugins").listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().startsWith("org.eclipse.platform")
                        && pathname.getName().endsWith(".jar");
            }
        });
        Assert.assertEquals(1, sitePlugins.length);

        // verify the bundle actually makes sense
        DefaultBundleReader reader = new DefaultBundleReader();
        OsgiManifest siteBundleManifest = reader.loadManifest(sitePlugins[0]);
        Assert.assertEquals("platform.jar", siteBundleManifest.getBundleClasspath()[0]);
        Assert.assertEquals("org.eclipse.platform", siteBundleManifest.getBundleSymbolicName());

        File[] productPlugins = new File(verifier.getBasedir(), "product/target/product/eclipse/plugins")
                .listFiles(new FileFilter() {
                    public boolean accept(File pathname) {
                        return pathname.isDirectory() && pathname.getName().startsWith("org.eclipse.platform");
                    }
                });
        Assert.assertEquals(1, productPlugins.length);

        // verify directory actually makes sense
        OsgiManifest productBundleManifest = reader.loadManifest(productPlugins[0]);
        Assert.assertEquals("platform.jar", productBundleManifest.getBundleClasspath()[0]);
        Assert.assertEquals("org.eclipse.platform", productBundleManifest.getBundleSymbolicName());

    }

}
