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
package org.eclipse.tycho.test.TYCHO134signJars;

import java.io.File;
import java.net.URL;

import junit.framework.Assert;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.UpdateSiteUtil;

/* java -jar \eclipse\plugins\org.eclipse.equinox.launcher_1.0.1.R33x_v20080118.jar -application org.eclipse.update.core.siteOptimizer -digestBuilder -digestOutputDir=d:\temp\eclipse\digest -siteXML=D:\sonatype\workspace\tycho\tycho-its\projects\tycho129\tycho.demo.site\target\site\site.xml  -jarProcessor -processAll -pack -outputDir d:\temp\eclipse\site D:\sonatype\workspace\tycho\tycho-its\projects\tycho129\tycho.demo.site\target\site */
public class SignJarsTestDisabled extends AbstractTychoIntegrationTest {

    //@Test
    public void signSite() throws Exception {
        Verifier verifier = getVerifier("/tycho134");
        verifier.getSystemProperties().setProperty("keystore", getResourceFile("tycho134/.keystore").getAbsolutePath());
        verifier.getSystemProperties().setProperty("storepass", "sonatype-keystore");
        verifier.getSystemProperties().setProperty("alias", "tycho");
        verifier.getSystemProperties().setProperty("keypass", "tycho-signing");

        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();

        File site = new File(verifier.getBasedir(), "tycho.demo.site/target/site");

        checkUpdatesite(verifier, site);

        checkPack200Files(site);

    }

    private void checkUpdatesite(Verifier verifier, File site) throws Exception {
        File mirror = new File(verifier.getBasedir(), "target/site");
        UpdateSiteUtil.mirrorSite(site, mirror);

        File siteXml = new File(mirror, "site.xml");
        Assert.assertTrue("Site.xml should be downloaded at mirror " + mirror, siteXml.exists());
        File feature = new File(mirror, "features/tycho.demo.feature_1.0.0.jar");
        Assert.assertTrue("Feature should be downloaded at mirror " + mirror, feature.exists());

        File plugin = new File(mirror, "plugins/tycho.demo_1.0.0.jar");
        Assert.assertTrue("Plugin should be downloaded at mirror " + mirror, plugin.exists());

    }

    private void checkPack200Files(File site) {
        File plugin = new File(site, "plugins/tycho.demo_1.0.0.jar.pack.gz");
        Assert.assertTrue("Plugin pack should exist " + plugin, plugin.exists());
    }

    private File getResourceFile(String relativePath) {
        URL root = AbstractTychoIntegrationTest.class.getResource("/");
        return new File(root.getFile(), relativePath);
    }

}
