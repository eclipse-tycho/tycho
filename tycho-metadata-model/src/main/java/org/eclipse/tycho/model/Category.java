/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
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
import java.util.List;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.tycho.model.UpdateSite.SiteFeatureRef;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;
import de.pdark.decentxml.XMLWriter;

public class Category {

    public static final String CATEGORY_XML = "category.xml";

    private static XMLParser parser = new XMLParser();

    private final Element dom;

    private final Document document;

    public Category(Document document) {
        this.document = document;
        this.dom = document.getRootElement();
    }

    public List<SiteFeatureRef> getFeatures() {
        ArrayList<SiteFeatureRef> features = new ArrayList<SiteFeatureRef>();
        for (Element featureDom : dom.getChildren("feature")) {
            features.add(new SiteFeatureRef(featureDom));
        }
        return Collections.unmodifiableList(features);
    }

    public List<PluginRef> getPlugins() {
        ArrayList<PluginRef> plugins = new ArrayList<PluginRef>();
        for (Element pluginDom : dom.getChildren("bundle")) {
            plugins.add(new PluginRef(pluginDom));
        }
        return Collections.unmodifiableList(plugins);
    }

    public static Category read(File file) throws IOException {
        return read(new BufferedInputStream(new FileInputStream(file)));
    }

    public static Category read(InputStream is) throws IOException {
        try {
            return new Category(parser.parse(new XMLIOSource(is)));
        } finally {
            IOUtil.close(is);
        }
    }

    public static void write(Category category, File file) throws IOException {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(file));

        Document document = category.document;
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

}
