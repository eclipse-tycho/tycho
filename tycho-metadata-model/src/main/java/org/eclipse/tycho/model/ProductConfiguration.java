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
package org.eclipse.tycho.model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import eu.maveniverse.domtrip.Attribute;
import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;
import eu.maveniverse.domtrip.Node;

/**
 * As of eclipse 3.5.1, file format does not seem to be documented. There are most likely multiple
 * parser implementations. org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile
 */
public class ProductConfiguration {

    public static ProductConfiguration read(File file) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        return read(is); // closes the stream
    }

    public static ProductConfiguration read(InputStream input) throws IOException {
        try (input) {
            return new ProductConfiguration(Document.of(input));
        }
    }

    public static void write(ProductConfiguration product, File file) throws IOException {
        Document document = product.document;
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
            document.toXml(os);
        }
    }

    private Element dom;

    private Document document;

    public ProductConfiguration(Document document) {
        this.document = document;
        this.dom = document.root();
    }

    public String getProduct() {
        return dom.attribute("id");
    }

    public String getApplication() {
        return dom.attribute("application");
    }

    public List<FeatureRef> getFeatures() throws ModelFileSyntaxException {
        Element featuresDom = dom.childElement("features").orElse(null);
        if (featuresDom == null) {
            return Collections.emptyList();
        }

        ArrayList<FeatureRef> features = new ArrayList<>();
        for (Element featureDom : featuresDom.childElements().toList()) {
            features.add(parseFeature(featureDom));
        }
        return Collections.unmodifiableList(features);
    }

    private static FeatureRef parseFeature(Element featureDom) throws ModelFileSyntaxException {
        // knowing the name of the parent element is useful for the error message, so we check the name here
        if (!"feature".equals(featureDom.name())) {
            throw new ModelFileSyntaxException(
                    "Invalid child element \"" + featureDom.name() + "\" in \"features\"");
        }
        return new FeatureRef(featureDom);
    }

    // TODO 428889 remove once p2 handles installMode="root" features
    public void removeRootInstalledFeatures() {
        Element featuresDom = dom.childElement("features").orElse(null);
        if (featuresDom != null) {

            for (int childIx = featuresDom.childCount() - 1; childIx > 0; --childIx) {
                Node nodeDom = featuresDom.getNode(childIx);

                if (nodeDom instanceof Element elementDom) {
                    if (parseFeature(elementDom).getInstallMode() == FeatureRef.InstallMode.root) {
                        featuresDom.removeChild(nodeDom);
                    }
                }
            }
        }
    }

    public String getId() {
        return dom.attribute("uid");
    }

    public Launcher getLauncher() {
        Element domLauncher = dom.childElement("launcher").orElse(null);
        if (domLauncher == null) {
            return null;
        }
        return new Launcher(domLauncher);
    }

    public String getName() {
        return dom.attribute("name");
    }

    public List<PluginRef> getPlugins() {
        Element pluginsDom = dom.childElement("plugins").orElse(null);
        if (pluginsDom == null) {
            return Collections.emptyList();
        }

        ArrayList<PluginRef> plugins = new ArrayList<>();
        for (Element pluginDom : pluginsDom.childElements("plugin").toList()) {
            plugins.add(new PluginRef(pluginDom));
        }
        return Collections.unmodifiableList(plugins);
    }

    public ProductType getType() {
        String type = dom.attribute("type");
        if (type != null && !type.isEmpty()) {
            return ProductType.parse(type);
        }
        // Support legacy 'useFeatures' attribute  
        String useFeatures = dom.attribute("useFeatures");
        if (useFeatures != null && !useFeatures.isEmpty()) {
            return Boolean.parseBoolean(useFeatures) ? ProductType.FEATURES : ProductType.BUNDLES;
        }
        return ProductType.BUNDLES;
    }

    public boolean includeLaunchers() {
        String attribute = dom.attribute("includeLaunchers");
        return attribute == null || Boolean.parseBoolean(attribute);
    }

    public boolean includeJRE() {
        String attribute = dom.attribute("includeJRE");
        return attribute != null && Boolean.parseBoolean(attribute);
    }

    public String getVersion() {
        return dom.attribute("version");
    }

    public void setVersion(String version) {
        dom.attribute("version", version);
    }

    public List<String> getW32Icons() {
        Element domLauncher = dom.childElement("launcher").orElse(null);
        if (domLauncher == null) {

            return null;
        }
        Element win = domLauncher.childElement("win").orElse(null);
        if (win == null) {
            return null;
        }
        List<String> icons = new ArrayList<>();
        String useIco = win.attribute("useIco");
        if (Boolean.valueOf(useIco)) {
            // for (Element ico : win.childElements("ico").toList())
            {
                Element ico = win.childElement("ico").orElse(null);
                // should be only 1
                icons.add(ico.attribute("path"));
            }
        } else {
            for (Element bmp : win.childElements("bmp").toList()) {
                List<Attribute> attibuteNames = new ArrayList<>(bmp.attributeObjects().values());
                if (attibuteNames != null && attibuteNames.size() > 0)
                    icons.add(attibuteNames.get(0).value());
            }
        }
        return icons;
    }

    public String getLinuxIcon() {
        Element domLauncher = dom.childElement("launcher").orElse(null);
        if (domLauncher == null) {

            return null;
        }
        Element linux = domLauncher.childElement("linux").orElse(null);
        if (linux == null) {
            return null;
        }

        return linux.attribute("icon");
    }

    public String getFreeBSDIcon() {
        Element domLauncher = dom.childElement("launcher").orElse(null);
        if (domLauncher == null) {

            return null;
        }
        Element freebsd = domLauncher.childElement("freebsd").orElse(null);
        if (freebsd == null) {
            return null;
        }

        return freebsd.attribute("icon");
    }

    public Map<String, BundleConfiguration> getPluginConfiguration() {
        Element configurationsDom = dom.childElement("configurations").orElse(null);
        if (configurationsDom == null) {
            return null;
        }

        Map<String, BundleConfiguration> configs = new HashMap<>();
        for (Element pluginDom : configurationsDom.childElements("plugin").toList()) {
            configs.put(pluginDom.attribute("id"), new BundleConfiguration(pluginDom));
        }
        return Collections.unmodifiableMap(configs);
    }

    public List<ConfigurationProperty> getConfigurationProperties() {
        Element configurationsDom = dom.childElement("configurations").orElse(null);
        if (configurationsDom == null) {
            return null;
        }

        List<Element> propertyDoms = configurationsDom.childElements("property").toList();
        if (propertyDoms == null) {
            return null;
        }

        List<ConfigurationProperty> properties = new ArrayList<>();
        for (Element properyDom : propertyDoms) {
            properties.add(new ConfigurationProperty(properyDom));
        }
        return Collections.unmodifiableList(properties);
    }

    public String getMacIcon() {
        Element domLauncher = dom.childElement("launcher").orElse(null);
        if (domLauncher == null) {

            return null;
        }
        Element linux = domLauncher.childElement("macosx").orElse(null);
        if (linux == null) {
            return null;
        }
        return linux.attribute("icon");
    }

    /**
     * @see org.eclipse.equinox.internal.p2.publisher.eclipse.ProductContentType
     */
    public enum ProductType {
        BUNDLES, // only bundles are accepted in the product
        FEATURES, // only features are accepted in the product
        MIXED; // all kinds of installable units are accepted in the product

        public static ProductType parse(String s) {
            try {
                return ProductType.valueOf(s.toUpperCase(Locale.ENGLISH));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Illegal product type " + s, e); //$NON-NLS-1$
            }
        }

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ENGLISH);
        }
    }

    public ConfigIni getConfigIni() {
        Element configIniElement = dom.childElement("configIni").orElse(null);
        if (configIniElement == null) {
            return null;
        }
        return new ConfigIni(configIniElement);
    }

    public static class ConfigIni {
        private String linuxConfigIni;
        private String freebsdConfigIni;
        private String macosxConfigIni;
        private String solarisConfigIni;
        private String win32ConfigIni;
        private boolean useDefault = true;

        private ConfigIni(Element configIniElement) {
            useDefault = "default".equals(configIniElement.attribute("use"));
            linuxConfigIni = getOsSpecificConfigIni(configIniElement, "linux");
            freebsdConfigIni = getOsSpecificConfigIni(configIniElement, "freebsd");
            macosxConfigIni = getOsSpecificConfigIni(configIniElement, "macosx");
            solarisConfigIni = getOsSpecificConfigIni(configIniElement, "solaris");
            win32ConfigIni = getOsSpecificConfigIni(configIniElement, "win32");
        }

        public boolean isUseDefault() {
            return useDefault;
        }

        private String getOsSpecificConfigIni(Element configIniElement, String os) {
            Element osElement = configIniElement.childElement(os).orElse(null);
            if (osElement != null) {
                String trimmedValue = osElement.textContentTrimmed();
                if (!trimmedValue.isEmpty()) {
                    return trimmedValue;
                }
            }
            return null;
        }

        public String getLinuxConfigIni() {
            return linuxConfigIni;
        }

        public String getFreeBSDConfigIni() {
            return freebsdConfigIni;
        }

        public String getMacosxConfigIni() {
            return macosxConfigIni;
        }

        public String getSolarisConfigIni() {
            return solarisConfigIni;
        }

        public String getWin32ConfigIni() {
            return win32ConfigIni;
        }

    }

    public static class ConfigurationProperty {
        private final Element dom;

        public ConfigurationProperty(Element dom) {
            this.dom = dom;
        }

        public String getName() {
            return dom.attribute("name");
        }

        public String getValue() {
            return dom.attribute("value");
        }

        public void setValue(String value) {
            dom.attribute("value", value);
        }
    }
}
