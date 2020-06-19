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
package org.eclipse.tycho.maven.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.Platform;
import org.eclipse.tycho.model.PluginRef;
import org.junit.Before;
import org.junit.Test;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLParser;

public class EclipseModelTest {

    File target = new File("target/modelio");

    @Before
    public void setUp() throws Exception {
        target.mkdirs();
    }

    @Test
    public void testFeature() throws Exception {
        Feature feature = Feature.read(new File("src/test/resources/modelio/feature.xml"));

        assertEquals("1.0.0", feature.getVersion());

        List<PluginRef> plugins = feature.getPlugins();
        assertEquals(1, plugins.size());
        assertEquals("pluginA", plugins.get(0).getId());

        List<FeatureRef> features = feature.getIncludedFeatures();
        assertEquals(1, features.size());

        List<Feature.RequiresRef> requires = feature.getRequires();
        assertEquals(1, requires.size());
        assertEquals("pluginB", requires.get(0).getImports().get(0).getPlugin());
        assertEquals("featureC", requires.get(0).getImports().get(1).getFeature());

        // not structural data - getters
        assertEquals("featureA", feature.getLabel());
        assertEquals("COMPANY", feature.getProvider());
        assertEquals("Test License", feature.getLicense().trim());
        assertEquals("http://www.example.com/license", feature.getLicenseURL());
        assertEquals(null, feature.getCopyrightURL());
        assertEquals(null, feature.getCopyright());

        feature.setVersion("1.2.3");
        plugins.get(0).setVersion("3.4.5");

        // not structural data - setters
        feature.setLabel("featureA_MODIFIED");
        feature.setProvider("COMPANY_MODIFIED");
        feature.setLicense("Test License MODIFIED");
        feature.setLicenseURL("http://www.example.com/license_MODIFIED");
        feature.setCopyright("Test Copyright");
        feature.setCopyrightURL("http://www.example.com/copyright");

        File updatedFile = new File(target, "feature.xml");
        Feature.write(feature, updatedFile);
        Feature updated = Feature.read(updatedFile);
        assertEquals("1.2.3", updated.getVersion());
        assertEquals("3.4.5", updated.getPlugins().get(0).getVersion());

        // not structural data - persistence
        assertEquals("featureA_MODIFIED", feature.getLabel());
        assertEquals("COMPANY_MODIFIED", feature.getProvider());
        assertEquals("Test License MODIFIED", feature.getLicense());
        assertEquals("http://www.example.com/license_MODIFIED", feature.getLicenseURL());
        assertEquals("http://www.example.com/copyright", feature.getCopyrightURL());
        assertEquals("Test Copyright", feature.getCopyright());
    }

    @Test
    public void testPlatform() throws Exception {
        Platform platform = Platform.read(new File("src/test/resources/modelio/platform.xml"));

        assertEquals(false, platform.isTransient());

        List<Platform.Site> sites = platform.getSites();
        assertEquals(2, sites.size());

        List<String> plugins = sites.get(0).getPlugins();
        assertEquals(2, plugins.size());
        assertEquals("m2eclipse/org.maven.ide.components.archetype-common/", plugins.get(0));

        List<Platform.Feature> features = sites.get(1).getFeatures();
        assertEquals(2, features.size());

        Platform transientPlatform = new Platform(platform);
        transientPlatform.setTransient(true);

        Platform.Site transientSite = new Platform.Site("file:/xxx");
        transientPlatform.addSite(transientSite);

        List<String> transientPlugins = new ArrayList<>();
        transientPlugins.add("plugins/yyy/");
        transientPlugins.add("plugins/zzz/");
        transientSite.setPlugins(transientPlugins);

        List<Platform.Feature> transientFeatures = new ArrayList<>();

        Platform.Feature transientFeature = new Platform.Feature();
        transientFeature.setId("transient.feature");
        transientFeature.setUrl("transient-url");
        transientFeature.setVersion("1.2.3");
        transientFeatures.add(transientFeature);

        transientSite.setFeatures(transientFeatures);

        File updatedFile = new File(target, "platform.xml");
        Platform.write(transientPlatform, updatedFile);

        Platform updated = Platform.read(updatedFile);

        assertEquals("plugins/yyy/,plugins/zzz/", updated.getSites().get(2).getPluginsStr());

        List<Platform.Feature> updatedFeatures = updated.getSites().get(2).getFeatures();
        assertEquals(1, updatedFeatures.size());
        assertEquals(transientFeature.getId(), updatedFeatures.get(0).getId());
        assertEquals(transientFeature.getUrl(), updatedFeatures.get(0).getUrl());
        assertEquals(transientFeature.getVersion(), updatedFeatures.get(0).getVersion());
    }

    @Test
    public void testDefaultXmlEncoding() throws Exception {
        // Run the test with -Dfile.encoding=Cp1252 to be sure

        Feature feature = Feature.read(new File("src/test/resources/modelio/feature-default-encoding.xml"));
        Feature.write(feature, new File("target/feature-default-encoding.xml"));

        Document document = XMLParser.parse(new File("target/feature-default-encoding.xml"));
        Element child = document.getChild("/feature/license");

        assertEquals("\u201cI AGREE\u201d", child.getText().trim());
    }
}
