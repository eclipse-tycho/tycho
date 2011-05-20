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
package org.eclipse.tycho.test.TYCHO197buildqualifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import junit.framework.Assert;

import org.apache.maven.it.Verifier;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class BuildQualifierTest extends AbstractTychoIntegrationTest {

    @Test
    public void checkProduct() throws Exception {
        Verifier verifier = getVerifier("/TYCHO197buildqualifier/product-test");

        final String timestamp = "20022002-2002";
        verifier.getSystemProperties().setProperty("forceContextQualifier", timestamp);
        verifier.executeGoal("install");
        verifier.verifyErrorFreeLog();

        File basedir = new File(verifier.getBasedir());

        final String version = "1.0.0." + timestamp;
        String featureLabel = "features/Feature_" + version;
        String pluginLabel = "plugins/Plugin_" + version + ".jar";

        File product = new File(basedir, "Product/target/product/eclipse");
        Assert.assertTrue("Product folder should exists", product.isDirectory());

        File feature = new File(product, featureLabel);
        Assert.assertTrue("Feature '" + featureLabel + "' should exists", feature.isDirectory());

        File featureJar = new File(feature, "feature.xml");
        Feature featureXml = Feature.read(new FileInputStream(featureJar));
        Assert.assertEquals("Invalid feature version", version, featureXml.getVersion());

        PluginRef pluginRef = featureXml.getPlugins().get(0);
        Assert.assertEquals("Invalid plugin version at feature.xml", version, pluginRef.getVersion());

        File plugin = new File(product, pluginLabel);
        Assert.assertTrue("Plugin '" + pluginLabel + "' should exists", plugin.isFile());

        Manifest man = readManifest(plugin);
        String bundleVersion = man.getMainAttributes().getValue("Bundle-Version");
        Assert.assertEquals("Invalid Bundle-Version at plugin Manifest.MF", version, bundleVersion);
    }

    @Test
    public void checkSite() throws Exception {
        Verifier verifier = getVerifier("/TYCHO197buildqualifier/site-test");

        final String timestamp = "20022002-2002";
        verifier.getSystemProperties().setProperty("forceContextQualifier", timestamp);
        verifier.executeGoal("install");
        verifier.verifyErrorFreeLog();

        File basedir = new File(verifier.getBasedir());

        final String version = "1.0.0." + timestamp;
        String featureLabel = "features/Feature_" + version;
        String pluginLabel = "plugins/Plugin_" + version + ".jar";
        featureLabel += ".jar";

        File site = new File(basedir, "Site/target/site");
        Assert.assertTrue("Site folder should exists", site.isDirectory());
        File siteXml = new File(site, "site.xml");
        Assert.assertTrue("Site.xml should exists", siteXml.isFile());
        String siteContet = readFileToString(siteXml).toString();
        Assert.assertTrue("Site.xml should contain '" + featureLabel + "'. Got:\n" + siteContet,
                siteContet.contains(featureLabel));

        File feature = new File(site, featureLabel);
        Assert.assertTrue("Feature '" + featureLabel + "' should exists", feature.isFile());

        Feature featureXml = readFeatureXml(feature);
        Assert.assertEquals("Invalid feature version", version, featureXml.getVersion());

        PluginRef pluginRef = featureXml.getPlugins().get(0);
        Assert.assertEquals("Invalid plugin version at feature.xml", version, pluginRef.getVersion());

        File plugin = new File(site, pluginLabel);
        Assert.assertTrue("Plugin '" + pluginLabel + "' should exists", plugin.isFile());

        Manifest man = readManifest(plugin);
        String bundleVersion = man.getMainAttributes().getValue("Bundle-Version");
        Assert.assertEquals("Invalid Bundle-Version at plugin Manifest.MF", version, bundleVersion);
    }

    private Feature readFeatureXml(File file) throws IOException, XmlPullParserException {
        ZipFile zip = new ZipFile(file);
        try {
            ZipEntry entry = zip.getEntry(Feature.FEATURE_XML);
            return Feature.read(zip.getInputStream(entry));
        } finally {
            zip.close();
        }
    }

    private Manifest readManifest(File file) throws IOException {
        JarFile jar = new JarFile(file);
        try {
            return jar.getManifest();
        } finally {
            jar.close();
        }
    }
}
