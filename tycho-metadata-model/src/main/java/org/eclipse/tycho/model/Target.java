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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codehaus.plexus.util.IOUtil;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;
import de.pdark.decentxml.XMLWriter;

public class Target {
    private static XMLParser parser = new XMLParser();

    private Element dom;

    private Document document;

    public static class Location {
        private final Element dom;

        public Location(Element dom) {
            this.dom = dom;
        }

        public List<Unit> getUnits() {
            ArrayList<Unit> units = new ArrayList<Unit>();
            for (Element unitDom : dom.getChildren("unit")) {
                units.add(new Unit(unitDom));
            }
            return Collections.unmodifiableList(units);
        }

        public List<Repository> getRepositories() {
            final List<Element> repositoryNodes = dom.getChildren("repository");

            final List<Repository> repositories = new ArrayList<Target.Repository>(repositoryNodes.size());
            for (Element node : repositoryNodes) {
                repositories.add(new Repository(node));
            }
            return repositories;
        }

        public String getType() {
            return dom.getAttributeValue("type");
        }

        public void setType(String type) {
            dom.setAttribute("type", type);
        }
    }

    public static final class Repository {
        private final Element dom;

        public Repository(Element dom) {
            this.dom = dom;
        }

        public String getId() {
            // this is Maven specific, used to match credentials and mirrors
            return dom.getAttributeValue("id");
        }

        public String getLocation() {
            return dom.getAttributeValue("location");
        }

        public void setLocation(String location) {
            dom.setAttribute("location", location);
        }
    }

    public static class Unit {
        private final Element dom;

        public Unit(Element dom) {
            this.dom = dom;
        }

        public String getId() {
            return dom.getAttributeValue("id");
        }

        public String getVersion() {
            return dom.getAttributeValue("version");
        }

        public void setVersion(String version) {
            dom.setAttribute("version", version);
        }
    }

    public Target(Document document) {
        this.document = document;
        this.dom = document.getRootElement();
    }

    public List<Location> getLocations() {
        ArrayList<Location> locations = new ArrayList<Location>();
        Element locationsDom = dom.getChild("locations");
        if (locationsDom != null) {
            for (Element locationDom : locationsDom.getChildren("location")) {
                locations.add(new Location(locationDom));
            }
        }
        return Collections.unmodifiableList(locations);
    }

    public static Target read(File file) throws IOException {
        FileInputStream input = new FileInputStream(file);
        try {
            return new Target(parser.parse(new XMLIOSource(input)));
        } finally {
            IOUtil.close(input);
        }
    }

    public static void write(Target target, File file) throws IOException {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(file));

        Document document = target.document;
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
