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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * http://help.eclipse.org/ganymede/topic/org.eclipse.platform.doc.isv/reference/misc/
 * update_platform_xml.html
 */
public class Platform {

    final Xpp3Dom dom;

    public Platform(Xpp3Dom dom) {
        this.dom = dom;
    }

    public Platform(Platform other) {
        this.dom = new Xpp3Dom(other.dom);

        setTimestamp(System.currentTimeMillis());
    }

    public Platform() {
        this.dom = new Xpp3Dom("config");

        dom.setAttribute("version", "3.0");
        setTimestamp(System.currentTimeMillis());
        setTransient(true);
    }

    public void setTimestamp(long timestamp) {
        dom.setAttribute("date", Long.toString(timestamp));
    }

    public static class Site {

        final Xpp3Dom dom;

        public Site(Xpp3Dom dom) {
            this.dom = dom;
        }

        public Site(String url) {
            this.dom = new Xpp3Dom("site");

            dom.setAttribute("url", url);
            dom.setAttribute("enabled", "true");
            dom.setAttribute("updateable", "true");
            dom.setAttribute("policy", "USER-INCLUDE");
        }

        public List<Feature> getFeatures() {
            ArrayList<Feature> features = new ArrayList<Feature>();
            for (Xpp3Dom featureDom : dom.getChildren("feature")) {
                features.add(new Feature(featureDom));
            }
            return Collections.unmodifiableList(features);
        }

        public List<String> getPlugins() {
            ArrayList<String> plugins = new ArrayList<String>();
            String pluginsStr = getPluginsStr();
            if (pluginsStr != null) {
                StringTokenizer st = new StringTokenizer(pluginsStr, ",");
                while (st.hasMoreTokens()) {
                    plugins.add(st.nextToken());
                }
            }
            return Collections.unmodifiableList(plugins);
        }

        public void setPlugins(List<String> plugins) {
            StringBuilder sb = new StringBuilder();
            for (String plugin : plugins) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(plugin);
            }
            setPluginsStr(sb.toString());
        }

        public String getPluginsStr() {
            return dom.getAttribute("list");
        }

        public void setPluginsStr(String plugins) {
            dom.setAttribute("list", plugins);
        }

        public void setFeatures(List<Feature> features) {
            for (int i = 0; i < dom.getChildCount();) {
                if ("feature".equals(dom.getChild(i).getName())) {
                    dom.removeChild(i);
                } else {
                    i++;
                }
            }
            if (features != null) {
                for (Feature feature : features) {
                    dom.addChild(feature.dom);
                }
            }
        }

    }

    public static class Feature {

        private final Xpp3Dom dom;

        public Feature(Xpp3Dom dom) {
            this.dom = dom;
        }

        public Feature() {
            this.dom = new Xpp3Dom("feature");
        }

        public void setId(String value) {
            dom.setAttribute("id", value);
        }

        public String getId() {
            return dom.getAttribute("id");
        }

        public void setVersion(String value) {
            dom.setAttribute("version", value);
        }

        public String getVersion() {
            return dom.getAttribute("version");
        }

        public void setUrl(String value) {
            dom.setAttribute("url", value);
        }

        public String getUrl() {
            return dom.getAttribute("url");
        }
    }

    @SuppressWarnings("deprecation")
    public static Platform read(File file) throws IOException, XmlPullParserException {
        XmlStreamReader reader = ReaderFactory.newXmlReader(file);
        try {
            return new Platform(Xpp3DomBuilder.build(reader));
        } finally {
            reader.close();
        }
    }

    public static void write(Platform platform, File file) throws IOException {
        file.getParentFile().mkdirs();
        Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
        try {
            Xpp3DomWriter.write(writer, platform.dom);
        } finally {
            writer.close();
        }
    }

    public boolean isTransient() {
        return Boolean.parseBoolean(dom.getAttribute("transient"));
    }

    public void setTransient(boolean value) {
        dom.setAttribute("transient", value ? "true" : "false");
    }

    public List<Site> getSites() {
        ArrayList<Site> sites = new ArrayList<Site>();
        for (Xpp3Dom siteDom : dom.getChildren("site")) {
            sites.add(new Site(siteDom));
        }
        return Collections.unmodifiableList(sites);
    }

    public void addSite(Site site) {
        dom.addChild(site.dom);
    }
}
