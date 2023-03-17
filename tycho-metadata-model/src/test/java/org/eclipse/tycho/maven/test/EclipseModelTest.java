/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.maven.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.model.UpdateSite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLParser;

class EclipseModelTest {

    File target = new File("target/modelio");

    @BeforeEach
    public void setUp() throws Exception {
        target.mkdirs();
    }

    @Test
    void testUpdateSite() throws Exception {
        UpdateSite site = UpdateSite.read(new File("src/test/resources/modelio/site.xml"));

        List<UpdateSite.SiteFeatureRef> features = site.getFeatures();
        assertEquals(2, features.size());
        assertEquals("featureB", features.get(1).getId());
        assertEquals("2.0.0", features.get(1).getVersion());

        Map<String, String> archives = site.getArchives();
        assertEquals(2, archives.size());
        assertEquals("http://www.company.com/updates/plugins/pluginA_1.0.0.jar",
                archives.get("plugins/pluginA_1.0.0.jar"));

        features.get(0).setVersion("3.0.0");

        site.removeArchives();
        assertTrue(site.getArchives().isEmpty());

        File updatedFile = new File(target, "site.xml");
        UpdateSite.write(site, updatedFile);
        UpdateSite updated = UpdateSite.read(updatedFile);
        assertEquals("3.0.0", updated.getFeatures().get(0).getVersion());
        assertTrue(updated.getArchives().isEmpty());
    }

    @Test
    void testFeature() throws Exception {
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
    void testDefaultXmlEncoding() throws Exception {
        // Run the test with -Dfile.encoding=Cp1252 to be sure

        Feature feature = Feature.read(new File("src/test/resources/modelio/feature-default-encoding.xml"));
        Feature.write(feature, new File("target/feature-default-encoding.xml"));

        Document document = XMLParser.parse(new File("target/feature-default-encoding.xml"));
        Element child = document.getChild("/feature/license");

        assertEquals("\u201cI AGREE\u201d", child.getText().trim());
    }
}
