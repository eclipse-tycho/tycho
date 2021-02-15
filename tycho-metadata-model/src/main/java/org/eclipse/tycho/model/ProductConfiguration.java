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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.util.IOUtil;

import de.pdark.decentxml.Attribute;
import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.Node;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;
import de.pdark.decentxml.XMLWriter;

/**
 * As of eclipse 3.5.1, file format does not seem to be documented. There are most likely multiple
 * parser implementations. org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile
 */
public class ProductConfiguration {
    private static final XMLParser parser = new XMLParser();

    public static ProductConfiguration read(File file) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        return read(is); // closes the stream
    }

    public static ProductConfiguration read(InputStream input) throws IOException {
        try {
            return new ProductConfiguration(parser.parse(new XMLIOSource(input)));
        } finally {
            IOUtil.close(input);
        }
    }

    public static void write(ProductConfiguration product, File file) throws IOException {
        Document document = product.document;
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
            String enc = document.getEncoding() != null ? document.getEncoding() : "UTF-8";
            Writer w = new OutputStreamWriter(os, enc);
            XMLWriter xw = new XMLWriter(w);
            try {
                document.toXML(xw);
            } finally {
                xw.flush();
            }
        }
    }

    private Element dom;

    private Document document;

    public ProductConfiguration(Document document) {
        this.document = document;
        this.dom = document.getRootElement();
    }

    public String getProduct() {
        return dom.getAttributeValue("id");
    }

    public String getApplication() {
        return dom.getAttributeValue("application");
    }

    public List<FeatureRef> getFeatures() throws ModelFileSyntaxException {
        Element featuresDom = dom.getChild("features");
        if (featuresDom == null) {
            return Collections.emptyList();
        }

        ArrayList<FeatureRef> features = new ArrayList<>();
        for (Element featureDom : featuresDom.getChildren()) {
            features.add(parseFeature(featureDom));
        }
        return Collections.unmodifiableList(features);
    }

    private static FeatureRef parseFeature(Element featureDom) throws ModelFileSyntaxException {
        // knowing the name of the parent element is useful for the error message, so we check the name here
        if (!"feature".equals(featureDom.getName())) {
            throw new ModelFileSyntaxException(
                    "Invalid child element \"" + featureDom.getName() + "\" in \"features\"");
        }
        return new FeatureRef(featureDom);
    }

    // TODO 428889 remove once p2 handles installMode="root" features
    public void removeRootInstalledFeatures() {
        Element featuresDom = dom.getChild("features");
        if (featuresDom != null) {

            for (int childIx = featuresDom.getNodes().size() - 1; childIx > 0; --childIx) {
                Node nodeDom = featuresDom.getNode(childIx);

                if (nodeDom instanceof Element) {
                    Element elementDom = (Element) nodeDom;

                    if (parseFeature(elementDom).getInstallMode() == FeatureRef.InstallMode.root) {
                        featuresDom.removeNode(childIx);
                    }
                }
            }
        }
    }

    public String getId() {
        return dom.getAttributeValue("uid");
    }

    public Launcher getLauncher() {
        Element domLauncher = dom.getChild("launcher");
        if (domLauncher == null) {
            return null;
        }
        return new Launcher(domLauncher);
    }

    public String getName() {
        return dom.getAttributeValue("name");
    }

    public List<PluginRef> getPlugins() {
        Element pluginsDom = dom.getChild("plugins");
        if (pluginsDom == null) {
            return Collections.emptyList();
        }

        ArrayList<PluginRef> plugins = new ArrayList<>();
        for (Element pluginDom : pluginsDom.getChildren("plugin")) {
            plugins.add(new PluginRef(pluginDom));
        }
        return Collections.unmodifiableList(plugins);
    }

    public boolean useFeatures() {
        return Boolean.parseBoolean(dom.getAttributeValue("useFeatures"));
    }

    public boolean includeLaunchers() {
        String attribute = dom.getAttributeValue("includeLaunchers");
        return attribute == null ? true : Boolean.parseBoolean(attribute);
    }

    public String getVersion() {
        return dom.getAttributeValue("version");
    }

    public void setVersion(String version) {
        dom.setAttribute("version", version);
    }

    public List<String> getW32Icons() {
        Element domLauncher = dom.getChild("launcher");
        if (domLauncher == null) {

            return null;
        }
        Element win = domLauncher.getChild("win");
        if (win == null) {
            return null;
        }
        List<String> icons = new ArrayList<>();
        String useIco = win.getAttributeValue("useIco");
        if (Boolean.valueOf(useIco)) {
            // for (Element ico : win.getChildren("ico"))
            {
                Element ico = win.getChild("ico");
                // should be only 1
                icons.add(ico.getAttributeValue("path"));
            }
        } else {
            for (Element bmp : win.getChildren("bmp")) {
                List<Attribute> attibuteNames = bmp.getAttributes();
                if (attibuteNames != null && attibuteNames.size() > 0)
                    icons.add(attibuteNames.get(0).getValue());
            }
        }
        return icons;
    }

    public String getLinuxIcon() {
        Element domLauncher = dom.getChild("launcher");
        if (domLauncher == null) {

            return null;
        }
        Element linux = domLauncher.getChild("linux");
        if (linux == null) {
            return null;
        }

        return linux.getAttributeValue("icon");
    }

    public String getFreeBSDIcon() {
        Element domLauncher = dom.getChild("launcher");
        if (domLauncher == null) {

            return null;
        }
        Element freebsd = domLauncher.getChild("freebsd");
        if (freebsd == null) {
            return null;
        }

        return freebsd.getAttributeValue("icon");
    }

    public Map<String, BundleConfiguration> getPluginConfiguration() {
        Element configurationsDom = dom.getChild("configurations");
        if (configurationsDom == null) {
            return null;
        }

        Map<String, BundleConfiguration> configs = new HashMap<>();
        for (Element pluginDom : configurationsDom.getChildren("plugin")) {
            configs.put(pluginDom.getAttributeValue("id"), new BundleConfiguration(pluginDom));
        }
        return Collections.unmodifiableMap(configs);
    }

    public List<ConfigurationProperty> getConfigurationProperties() {
        Element configurationsDom = dom.getChild("configurations");
        if (configurationsDom == null) {
            return null;
        }

        List<Element> propertyDoms = configurationsDom.getChildren("property");
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
        Element domLauncher = dom.getChild("launcher");
        if (domLauncher == null) {

            return null;
        }
        Element linux = domLauncher.getChild("macosx");
        if (linux == null) {
            return null;
        }
        return linux.getAttributeValue("icon");
    }

    public ConfigIni getConfigIni() {
        Element configIniElement = dom.getChild("configIni");
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
            useDefault = "default".equals(configIniElement.getAttributeValue("use"));
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
            Element osElement = configIniElement.getChild(os);
            if (osElement != null) {
                String trimmedValue = osElement.getTrimmedText();
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
            return dom.getAttributeValue("name");
        }

        public String getValue() {
            return dom.getAttributeValue("value");
        }

        public void setValue(String value) {
            dom.setAttribute("value", value);
        }
    }
}
