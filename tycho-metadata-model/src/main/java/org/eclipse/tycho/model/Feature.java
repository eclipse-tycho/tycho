/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Uwe Stieber (Wind River) - [397160] Feature.ImportRef misses API to access all import references attributes
 *******************************************************************************/
package org.eclipse.tycho.model;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.codehaus.plexus.util.IOUtil;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;
import de.pdark.decentxml.XMLWriter;

/**
 * http://help.eclipse.org/ganymede/topic/org.eclipse.platform.doc.isv/reference/misc/
 * feature_manifest.html
 */
public class Feature {

    public static final String FEATURE_XML = "feature.xml";

    private static XMLParser parser = new XMLParser();

    private final Document document;

    private final Element dom;

    private ArrayList<PluginRef> plugins;

    private ArrayList<FeatureRef> features;

    public Feature(Document document) {
        this.document = document;
        this.dom = document.getRootElement();
    }

    /** copy constructor */
    public Feature(Feature other) {
        this(other.document.copy());
    }

    public List<PluginRef> getPlugins() {
        if (plugins == null) {
            plugins = new ArrayList<PluginRef>();
            for (Element pluginDom : dom.getChildren("plugin")) {
                plugins.add(new PluginRef(pluginDom));
            }
        }
        return Collections.unmodifiableList(plugins);
    }

    public void setVersion(String version) {
        dom.setAttribute("version", version);
    }

    public List<FeatureRef> getIncludedFeatures() {
        if (features == null) {
            features = new ArrayList<FeatureRef>();
            for (Element featureDom : dom.getChildren("includes")) {
                features.add(new FeatureRef(featureDom));
            }
        }
        return Collections.unmodifiableList(features);
    }

    public List<RequiresRef> getRequires() {
        ArrayList<RequiresRef> requires = new ArrayList<RequiresRef>();
        for (Element requiresDom : dom.getChildren("requires")) {
            requires.add(new RequiresRef(requiresDom));
        }
        return Collections.unmodifiableList(requires);
    }

    public static class RequiresRef {

        private final Element dom;

        public RequiresRef(Element dom) {
            this.dom = dom;
        }

        public List<ImportRef> getImports() {
            ArrayList<ImportRef> imports = new ArrayList<ImportRef>();
            for (Element importsDom : dom.getChildren("import")) {
                imports.add(new ImportRef(importsDom));
            }
            return Collections.unmodifiableList(imports);
        }

    }

    public static class ImportRef {

        private final Element dom;

        public ImportRef(Element dom) {
            this.dom = dom;
        }

        public String getPlugin() {
            return dom.getAttributeValue("plugin");
        }

        public String getFeature() {
            return dom.getAttributeValue("feature");
        }

        public String getVersion() {
            return dom.getAttributeValue("version");
        }

        public String getMatch() {
            String match = dom.getAttributeValue("match");
            if (match == null) {
                return "compatible";
            } else {
                return match;
            }
        }

        public String getPatch() {
            String patch = dom.getAttributeValue("patch");
            if (patch == null) {
                return "false";
            } else {
                return patch;
            }
        }
    }

    public String getVersion() {
        return dom.getAttributeValue("version");
    }

    public String getId() {
        return dom.getAttributeValue("id");
    }

    public void setId(String id) {
        dom.setAttribute("id", id);
    }

    public String getLicenseFeature() {
        return dom.getAttributeValue("license-feature");
    }

    public void setLicenseFeature(String featureId) {
        if (featureId != null) {
            dom.setAttribute("license-feature", featureId);
        } else {
            dom.removeAttribute("license-feature");
        }
    }

    public String getLicenseFeatureVersion() {
        return dom.getAttributeValue("license-feature-version");
    }

    public void setLicenseFeatureVersion(String version) {
        if (version != null) {
            dom.setAttribute("license-feature-version", version);
        } else {
            dom.removeAttribute("license-feature-version");
        }
    }

    public static Feature read(File file) throws IOException {
        FileInputStream is = new FileInputStream(file);
        return read(is); // closes the stream
    }

    public static Feature read(InputStream input) throws IOException {
        try {
            return new Feature(parser.parse(new XMLIOSource(input)));
        } finally {
            IOUtil.close(input);
        }
    }

    public static void write(Feature feature, File file) throws IOException {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(file));

        Document document = feature.document;
        try {
            String enc = document.getEncoding() != null ? document.getEncoding() : "UTF-8";
            Writer w = new OutputStreamWriter(os, enc);
            XMLWriter xw = new XMLWriter(w);
            try {
                document.toXML(xw);
            } finally {
                xw.flush();
            }
        } finally {
            IOUtil.close(os);
        }
    }

    public static Feature readJar(File file) throws IOException {
        JarFile jar = new JarFile(file);
        try {
            ZipEntry ze = jar.getEntry(FEATURE_XML);
            if (ze != null) {
                InputStream is = jar.getInputStream(ze);
                return read(is);
            }
            throw new IOException(file.getAbsolutePath() + " does not have " + FEATURE_XML + " entry.");
        } finally {
            jar.close();
        }
    }

    /**
     * Convenience method to load feature.xml file from either feature jar file or directory.
     * 
     * @throws RuntimeException
     *             if feature descriptor can not be read or parsed.
     */
    public static Feature loadFeature(File location) {
        try {
            Feature feature;
            if (location.isDirectory()) {
                feature = Feature.read(new File(location, Feature.FEATURE_XML));
            } else {
                // eclipse does NOT support packed features
                feature = Feature.readJar(location);
            }
            return feature;
        } catch (IOException e) {
            throw new RuntimeException("Could not read feature descriptor at " + location.getAbsolutePath(), e);
        }
    }

    public void addPlugin(PluginRef plugin) {
        dom.addNode(plugin.getDom());
        plugins = null;
    }

    public void addFeatureRef(FeatureRef feature) {
        dom.addNode(feature.getDom());
        features = null;
    }

    //////
    // Other (not structural) feature content
    //////

    // label
    public String getLabel() {
        return dom.getAttributeValue("label");
    }

    public void setLabel(String label) {
        dom.setAttribute("label", label);
    }

    // provider
    public String getProvider() {
        return dom.getAttributeValue("provider-name");
    }

    public void setProvider(String provider) {
        dom.setAttribute("provider-name", provider);
    }

    // description + url
    public String getDescription() {
        Element descElement = dom.getChild("description");
        if (descElement != null) {
            return descElement.getText();
        }
        return null;
    }

    public void setDescription(String description) {
        Element descElement = dom.getChild("description");
        if (descElement == null) {
            descElement = new Element("description");
            dom.addNode(descElement);
        }
        descElement.setText(description);
    }

    public String getDescriptionURL() {
        Element descElement = dom.getChild("description");
        if (descElement != null) {
            return descElement.getAttributeValue("url");
        }
        return null;
    }

    public void setDescriptionURL(String descriptionURL) {
        Element descElement = dom.getChild("description");
        if (descElement == null) {
            descElement = new Element("description");
            dom.addNode(descElement);
        }
        descElement.setAttribute("url", descriptionURL);
    }

    // copyright + url
    public String getCopyright() {
        Element copyrightElement = dom.getChild("copyright");
        if (copyrightElement != null) {
            return copyrightElement.getText();
        }
        return null;
    }

    public void setCopyright(String description) {
        Element copyrightElement = dom.getChild("copyright");
        if (copyrightElement == null) {
            copyrightElement = new Element("copyright");
            dom.addNode(copyrightElement);
        }
        copyrightElement.setText(description);
    }

    public String getCopyrightURL() {
        Element copyrightElement = dom.getChild("copyright");
        if (copyrightElement != null) {
            return copyrightElement.getAttributeValue("url");
        }
        return null;
    }

    public void setCopyrightURL(String copyrightURL) {
        Element copyrightElement = dom.getChild("copyright");
        if (copyrightElement == null) {
            copyrightElement = new Element("copyright");
            dom.addNode(copyrightElement);
        }
        copyrightElement.setAttribute("url", copyrightURL);
    }

    // license + url
    public String getLicense() {
        Element licenseElement = dom.getChild("license");
        if (licenseElement != null) {
            return licenseElement.getText();
        }
        return null;
    }

    public void setLicense(String license) {
        Element licenseElement = dom.getChild("license");
        if (licenseElement == null) {
            licenseElement = new Element("license");
            dom.addNode(licenseElement);
        }
        licenseElement.setText(license);
    }

    public String getLicenseURL() {
        Element licenseElement = dom.getChild("license");
        if (licenseElement != null) {
            return licenseElement.getAttributeValue("url");
        }
        return null;
    }

    public void setLicenseURL(String licenseURL) {
        Element licenseElement = dom.getChild("license");
        if (licenseElement == null) {
            licenseElement = new Element("license");
            dom.addNode(licenseElement);
        }
        licenseElement.setAttribute("url", licenseURL);
    }
}
