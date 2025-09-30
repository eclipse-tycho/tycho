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
import java.util.Map;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;
import eu.maveniverse.domtrip.Serializer;

/**
 * https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/misc/update_sitemap
 * .html
 */
public class UpdateSite {
    public static final String SITE_XML = "site.xml";

    private static Serializer serializer = new Serializer();

    private final Element dom;

    private final Document document;

    private String associateSitesUrl;

    public UpdateSite(Document document) {
        this.document = document;
        this.dom = document.root();

        if (dom.attribute("associateSitesURL") != null) {
            associateSitesUrl = dom.attribute("associateSitesURL");
        }
    }

    public List<SiteFeatureRef> getFeatures() {
        ArrayList<SiteFeatureRef> features = new ArrayList<>();
        for (Element featureDom : dom.children("feature").toList()) {
            features.add(new SiteFeatureRef(featureDom));
        }
        return Collections.unmodifiableList(features);
    }

    public Map<String, String> getArchives() {
        Map<String, String> archives = new HashMap<>();
        for (Element archiveDom : dom.children("archive").toList()) {
            String path = archiveDom.attribute("path");
            String url = archiveDom.attribute("url");
            archives.put(path, url);
        }
        return Collections.unmodifiableMap(archives);
    }

    public void removeArchives() {
        for (Element archive : dom.children("archive").toList()) {
            dom.removeNode(archive);
        }
    }

    public static class SiteFeatureRef extends FeatureRef {

        public SiteFeatureRef(Element dom) {
            super(dom);
        }

        public void setUrl(String url) {
            dom.attribute("url", url);
        }

        public String getUrl() {
            return dom.attribute("url");
        }

    }

    public static UpdateSite read(File file) throws IOException {
        return read(new BufferedInputStream(new FileInputStream(file)));
    }

    public static UpdateSite read(InputStream is) throws IOException {
        try (is) {
            return new UpdateSite(Document.of(is));
        }
    }

    public static void write(UpdateSite site, File file) throws IOException {
        Document document = site.document;
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
            String enc = document.encoding() != null ? document.encoding() : "UTF-8";
            serializer.serialize(document, os, enc);
        }
    }

    public String getAssociateSitesUrl() {
        return associateSitesUrl;
    }
}
