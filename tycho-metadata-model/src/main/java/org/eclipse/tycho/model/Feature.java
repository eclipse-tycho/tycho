/*******************************************************************************
 * Copyright (c) 2008, 2023 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.apache.commons.codec.digest.DigestUtils;
import org.osgi.framework.Version;
import org.osgi.resource.Resource;

import aQute.bnd.version.VersionRange;
import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;

/**
 * https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/misc/
 * feature_manifest.html
 */
public class Feature {

    /**
     * dependent plug-in version must be at least at the version specified, or at a higher service,
     * minor or major level.
     */
    public static final String MATCH_GREATER_OR_EQUAL = "greaterOrEqual";
    /**
     * dependent plug-in version must be at least at the version specified, or at a higher service level
     * or minor level (major version level must equal the specified version).
     */
    public static final String MATCH_COMPATIBLE = "compatible";
    /**
     * dependent plug-in version must be at least at the version specified, or at a higher service level
     * (major and minor version levels must equal the specified version).
     */
    public static final String MATCH_EQUIVALENT = "equivalent";
    /**
     * dependent plug-in version must match exactly the specified version. If "patch" is "true",
     * "perfect" is assumed and other values cannot be set.
     */
    public static final String MATCH_PERFECT = "perfect";

    public static final String FEATURE_XML = "feature.xml";

    private final Document document;

    private final Element dom;

    private ArrayList<PluginRef> plugins;

    private ArrayList<FeatureRef> features;

    private File source;

    public Feature(Document document) {
        this.document = document;
        this.dom = document.root();
    }

    /** copy constructor */
    public Feature(Feature other) {
        this(other.document.clone());
    }

    public List<PluginRef> getPlugins() {
        if (plugins == null) {
            plugins = new ArrayList<>();
            for (Element pluginDom : dom.children("plugin").toList()) {
                plugins.add(new PluginRef(pluginDom));
            }
        }
        return Collections.unmodifiableList(plugins);
    }

    public void setVersion(String version) {
        dom.attribute("version", version);
    }

    public List<FeatureRef> getIncludedFeatures() {
        if (features == null) {
            features = new ArrayList<>();
            for (Element featureDom : dom.children("includes").toList()) {
                features.add(new FeatureRef(featureDom));
            }
        }
        return Collections.unmodifiableList(features);
    }

    public List<RequiresRef> getRequires() {
        ArrayList<RequiresRef> requires = new ArrayList<>();
        for (Element requiresDom : dom.children("requires").toList()) {
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
            ArrayList<ImportRef> imports = new ArrayList<>();
            for (Element importsDom : dom.children("import").toList()) {
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
            return dom.attribute("plugin");
        }

        public String getFeature() {
            return dom.attribute("feature");
        }

        public String getVersion() {
            return dom.attribute("version");
        }

        public void setVersion(String version) {
            dom.attribute("version", version);
        }

        public String getMatch() {
            String match = dom.attribute("match");
            if (match == null || match.isBlank()) {
                return "compatible";
            } else {
                return match;
            }
        }

        public void setMatch(String match) {
            dom.attribute("match", match);
        }

        public String getPatch() {
            String patch = dom.attribute("patch");
            if (patch == null) {
                return "false";
            } else {
                return patch;
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ImportRef [");
            String plugin = getPlugin();
            if (plugin != null) {
                builder.append("plugin=");
                builder.append(plugin);
                builder.append(", ");
            }
            String feature = getFeature();
            if (feature != null) {
                builder.append("Feature=");
                builder.append(feature);
                builder.append(", ");
            }
            String version = getVersion();
            if (version != null) {
                builder.append("version=");
                builder.append(version);
                builder.append(", ");
            }
            String match = getMatch();
            if (match != null) {
                builder.append("match=");
                builder.append(match);
                builder.append(", ");
            }
            String patch = getPatch();
            if (patch != null) {
                builder.append("patch=");
                builder.append(patch);
            }
            builder.append("]");
            return builder.toString();
        }
    }

    public String getVersion() {
        return dom.attribute("version");
    }

    public String getId() {
        return dom.attribute("id");
    }

    public void setId(String id) {
        dom.attribute("id", id);
    }

    public String getBrandingPluginId() {
        return dom.attribute("plugin");
    }

    public void setBrandingPluginId(String id) {
        dom.attribute("plugin", id);
    }

    public String getLicenseFeature() {
        return dom.attribute("license-feature");
    }

    public void setLicenseFeature(String featureId) {
        if (featureId != null) {
            dom.attribute("license-feature", featureId);
        } else {
            dom.removeAttribute("license-feature");
        }
    }

    public String getLicenseFeatureVersion() {
        return dom.attribute("license-feature-version");
    }

    public void setLicenseFeatureVersion(String version) {
        if (version != null) {
            dom.attribute("license-feature-version", version);
        } else {
            dom.removeAttribute("license-feature-version");
        }
    }

    public static Feature read(File file) throws IOException {
        FileInputStream is = new FileInputStream(file);
        return read(is); // closes the stream
    }

    public static Feature read(InputStream input) throws IOException {
        try (input) {
            return new Feature(Document.of(input));
        }
    }

    public static void write(Feature feature, File file) throws IOException {
        write(feature, file, null);
    }

    public static void write(Feature feature, File file, String indent) throws IOException {
        Document document = feature.document;
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
            document.toXml(os);
        }
    }

    public static Feature readJar(File file) throws IOException {
        try (JarFile jar = new JarFile(file)) {
            ZipEntry ze = jar.getEntry(FEATURE_XML);
            if (ze != null) {
                InputStream is = jar.getInputStream(ze);
                Feature read = read(is);
                read.source = file;
                return read;
            }
            throw new IOException(file.getAbsolutePath() + " does not have " + FEATURE_XML + " entry.");
        }
    }

    /**
     * Convenience method to load feature.xml file from either feature jar file or directory.
     * 
     * @throws RuntimeException
     *                              if feature descriptor can not be read or parsed.
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
        return dom.attribute("label");
    }

    public void setLabel(String label) {
        dom.attribute("label", label);
    }

    // provider
    public String getProvider() {
        return dom.attribute("provider-name");
    }

    public void setProvider(String provider) {
        dom.attribute("provider-name", provider);
    }

    // description + url
    public String getDescription() {
        Element descElement = dom.child("description").orElse(null);
        if (descElement != null) {
            return descElement.textContent();
        }
        return null;
    }

    public void setDescription(String description) {
        Element descElement = dom.child("description").orElse(null);
        if (descElement == null) {
            descElement = Element.of("description");
            dom.addNode(descElement);
        }
        descElement.textContent(description);
    }

    public String getDescriptionURL() {
        Element descElement = dom.child("description").orElse(null);
        if (descElement != null) {
            return descElement.attribute("url");
        }
        return null;
    }

    public void setDescriptionURL(String descriptionURL) {
        Element descElement = dom.child("description").orElse(null);
        if (descElement == null) {
            descElement = Element.of("description");
            dom.addNode(descElement);
        }
        descElement.attribute("url", descriptionURL);
    }

    // copyright + url
    public String getCopyright() {
        Element copyrightElement = dom.child("copyright").orElse(null);
        if (copyrightElement != null) {
            return copyrightElement.textContent();
        }
        return null;
    }

    public void setCopyright(String description) {
        Element copyrightElement = dom.child("copyright").orElse(null);
        if (copyrightElement == null) {
            copyrightElement = Element.of("copyright");
            dom.addNode(copyrightElement);
        }
        copyrightElement.textContent(description);
    }

    public String getCopyrightURL() {
        Element copyrightElement = dom.child("copyright").orElse(null);
        if (copyrightElement != null) {
            return copyrightElement.attribute("url");
        }
        return null;
    }

    public void setCopyrightURL(String copyrightURL) {
        Element copyrightElement = dom.child("copyright").orElse(null);
        if (copyrightElement == null) {
            copyrightElement = Element.of("copyright");
            dom.addNode(copyrightElement);
        }
        copyrightElement.attribute("url", copyrightURL);
    }

    // license + url
    public String getLicense() {
        Element licenseElement = dom.child("license").orElse(null);
        if (licenseElement != null) {
            return licenseElement.textContent();
        }
        return null;
    }

    public void setLicense(String license) {
        Element licenseElement = dom.child("license").orElse(null);
        if (licenseElement == null) {
            licenseElement = Element.of("license");
            dom.addNode(licenseElement);
        }
        licenseElement.textContent(license);
    }

    public String getLicenseURL() {
        Element licenseElement = dom.child("license").orElse(null);
        if (licenseElement != null) {
            return licenseElement.attribute("url");
        }
        return null;
    }

    public void setLicenseURL(String licenseURL) {
        Element licenseElement = dom.child("license").orElse(null);
        if (licenseElement == null) {
            licenseElement = Element.of("license");
            dom.addNode(licenseElement);
        }
        licenseElement.attribute("url", licenseURL);
    }

    public String getOS() {
        return dom.attribute("os");
    }

    public void setOS(String value) {
        dom.attribute("os", value);
    }

    public String getArch() {
        return dom.attribute("arch");
    }

    public void setArch(String value) {
        dom.attribute("arch", value);
    }

    public String getWS() {
        return dom.attribute("ws");
    }

    public void setWS(String value) {
        dom.attribute("ws", value);
    }

    public Stream<Resource> toResource() {
        return Stream.of(toJarResource(), toGroupResource());
    }

    public Resource toGroupResource() {
        FeatureResourceBuilder featureGroup = new FeatureResourceBuilder();
        featureGroup.addFeatureGroupCapability(getId(), getVersion());
        featureGroup.addFeatureJarRequirement(getId(), getVersion());
        for (PluginRef pluginRef : getPlugins()) {
            featureGroup.addRequireBundle(pluginRef.getId(),
                    new VersionRange("[" + pluginRef.getVersion() + "," + pluginRef.getVersion() + "]"));
        }
        for (FeatureRef featureRef : getIncludedFeatures()) {
            featureGroup.addRequireFeature(featureRef.getId(),
                    "[" + featureRef.getVersion() + "," + featureRef.getVersion() + "]");

        }
        for (RequiresRef requiresRef : getRequires()) {
            for (ImportRef importRef : requiresRef.getImports()) {
                String feature = importRef.getFeature();
                String plugin = importRef.getPlugin();
                VersionRange range = getVersionRange(importRef);
                if (feature != null && !features.isEmpty()) {
                    //feature requirement
                    featureGroup.addRuntimeFeature(feature, range);
                } else if (plugin != null && !plugin.isEmpty()) {
                    //bundle requirements
                    featureGroup.addRuntimeBundleRequirement(plugin, range);
                }
            }
        }
        return featureGroup.build();

    }

    private static VersionRange getVersionRange(ImportRef importRef) {
        String version = importRef.getVersion();
        if (version == null || version.isEmpty()) {
            return new VersionRange(Version.emptyVersion.toString());
        }
        Version parsed = Version.parseVersion(version);
        String match = importRef.getMatch();
        if (MATCH_PERFECT.equals(match)) {
            return new VersionRange("[" + parsed + "," + parsed + "]");
        }
        if (MATCH_EQUIVALENT.equals(match)) {
            return new VersionRange("[" + parsed + "," + (parsed.getMajor() + "." + parsed.getMinor() + 1) + ")");
        }
        if (MATCH_COMPATIBLE.equals(match)) {
            return new VersionRange("[" + parsed + "," + (parsed.getMajor() + 1) + ")");
        }
        if (MATCH_GREATER_OR_EQUAL.equals(match)) {
            return new VersionRange(version);
        }
        throw new IllegalArgumentException("Can't parse value of match = " + match);
    }

    public Resource toJarResource() {
        FeatureResourceBuilder featureJar = new FeatureResourceBuilder();
        featureJar.addFeatureJarCapability(getId(), getVersion());
        if (source != null) {
            String sha256;
            try (FileInputStream data = new FileInputStream(source)) {
                sha256 = DigestUtils.sha256Hex(data);
            } catch (IOException e) {
                throw new RuntimeException("Computing digest failed!", e);
            }
            featureJar.addContentCapability(source.toURI(), sha256, source.length(), "application/eclipse-feature");
        }
        return featureJar.build();
    }
}
