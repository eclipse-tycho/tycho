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
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;
import de.pdark.decentxml.XMLWriter;

/**
 * As of eclipse 3.5.1, file format does not seem to be documented. There are most likely multiple
 * parser implementations. org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile
 */
public class ProductConfiguration {
    private static XMLParser parser = new XMLParser();

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
        OutputStream os = new BufferedOutputStream(new FileOutputStream(file));

        Document document = product.document;
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

    public List<FeatureRef> getFeatures() {
        Element featuresDom = dom.getChild("features");
        if (featuresDom == null) {
            return Collections.emptyList();
        }

        ArrayList<FeatureRef> features = new ArrayList<FeatureRef>();
        for (Element pluginDom : featuresDom.getChildren("feature")) {
            features.add(new FeatureRef(pluginDom));
        }
        return Collections.unmodifiableList(features);
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

        ArrayList<PluginRef> plugins = new ArrayList<PluginRef>();
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
        List<String> icons = new ArrayList<String>();
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

    public Map<String, BundleConfiguration> getPluginConfiguration() {
        Element configurationsDom = dom.getChild("configurations");
        if (configurationsDom == null) {
            return null;
        }

        Map<String, BundleConfiguration> configs = new HashMap<String, BundleConfiguration>();
        for (Element pluginDom : configurationsDom.getChildren("plugin")) {
            configs.put(pluginDom.getAttributeValue("id"), new BundleConfiguration(pluginDom));
        }
        return Collections.unmodifiableMap(configs);
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
        private String macosxConfigIni;
        private String solarisConfigIni;
        private String win32ConfigIni;
        private boolean useDefault = true;

        private ConfigIni(Element configIniElement) {
            useDefault = "default".equals(configIniElement.getAttribute("use"));
            linuxConfigIni = getOsSpecificConfigIni(configIniElement, "linux");
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
                if (trimmedValue.length() > 0) {
                    return trimmedValue;
                }
            }
            return null;
        }

        public String getLinuxConfigIni() {
            return linuxConfigIni;
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

}
