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
package org.eclipse.tycho.p2.resolver;

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
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionSyntaxException;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParseException;
import de.pdark.decentxml.XMLParser;
import de.pdark.decentxml.XMLWriter;

public class TargetDefinitionFile implements TargetDefinition {

    private static XMLParser parser = new XMLParser();

    private Element dom;

    private Document document;

    private String targetFileHash;

    public static class IULocation implements TargetDefinition.InstallableUnitLocation {
        private final Element dom;

        public IULocation(Element dom) {
            this.dom = dom;
        }

        public List<? extends TargetDefinition.Unit> getUnits() {
            ArrayList<Unit> units = new ArrayList<Unit>();
            for (Element unitDom : dom.getChildren("unit")) {
                units.add(new Unit(unitDom));
            }
            return Collections.unmodifiableList(units);
        }

        public List<? extends TargetDefinition.Repository> getRepositories() {
            return getRepositoryImpls();
        }

        public List<Repository> getRepositoryImpls() {
            final List<Element> repositoryNodes = dom.getChildren("repository");

            final List<Repository> repositories = new ArrayList<TargetDefinitionFile.Repository>(repositoryNodes.size());
            for (Element node : repositoryNodes) {
                repositories.add(new Repository(node));
            }
            return repositories;
        }

        public String getTypeDescription() {
            return dom.getAttributeValue("type");
        }

        public void setType(String type) {
            dom.setAttribute("type", type);
        }

        public IncludeMode getIncludeMode() {
            String attributeValue = dom.getAttributeValue("includeMode");
            if ("slicer".equals(attributeValue)) {
                return IncludeMode.SLICER;
            } else if ("planner".equals(attributeValue) || attributeValue == null) {
                return IncludeMode.PLANNER;
            }
            throw new TargetDefinitionSyntaxException("Invalid value for attribute 'includeMode': " + attributeValue
                    + "");
        }

        public boolean includeAllEnvironments() {
            return Boolean.parseBoolean(dom.getAttributeValue("includeAllPlatforms"));
        }

    }

    public class OtherLocation implements Location {
        private final String description;

        public OtherLocation(String description) {
            this.description = description;
        }

        public String getTypeDescription() {
            return description;
        }
    }

    public static final class Repository implements TargetDefinition.Repository {
        private final Element dom;

        public Repository(Element dom) {
            this.dom = dom;
        }

        public String getId() {
            // this is Maven specific, used to match credentials and mirrors
            return dom.getAttributeValue("id");
        }

        public URI getLocation() {
            try {
                return new URI(dom.getAttributeValue("location"));
            } catch (URISyntaxException e) {
                // this should be checked earlier (but is currently ugly to do)
                throw new RuntimeException(e);
            }
        }

        public void setLocation(String location) {
            dom.setAttribute("location", location);
        }
    }

    public static class Unit implements TargetDefinition.Unit {
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

    public TargetDefinitionFile(Document document) {
        this.document = document;
        this.dom = document.getRootElement();
    }

    public List<? extends TargetDefinition.Location> getLocations() {
        ArrayList<TargetDefinition.Location> locations = new ArrayList<TargetDefinition.Location>();
        Element locationsDom = dom.getChild("locations");
        if (locationsDom != null) {
            for (Element locationDom : locationsDom.getChildren("location")) {
                String type = locationDom.getAttributeValue("type");
                if ("InstallableUnit".equals(type))
                    locations.add(new IULocation(locationDom));
                else
                    locations.add(new OtherLocation(type));
            }
        }
        return Collections.unmodifiableList(locations);
    }

    public static TargetDefinitionFile read(File file) throws IOException {
        FileInputStream input = new FileInputStream(file);
        try {
            TargetDefinitionFile targetDef = new TargetDefinitionFile(parser.parse(new XMLIOSource(input)));
            String hash = getHashCode(file);
            targetDef.setTargetFileHash(hash);
            return targetDef;
        } catch (XMLParseException e) {
            throw new TargetDefinitionSyntaxException("Target definition is not well-formed XML: " + e.getMessage(), e);
        } finally {
            IOUtil.close(input);
        }
    }

    public static void write(TargetDefinitionFile target, File file) throws IOException {
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

    public static String getHashCode(File f) throws IOException {
        String result = null;
        MessageDigest digest = null;
        InputStream is = null;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            is = new BufferedInputStream(new FileInputStream(f));
            byte buffer[] = new byte[8192];
            for (int read = 0; (read = is.read(buffer)) > 0;)
                digest.update(buffer, 0, read);

            byte md5sum[] = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            result = bigInt.toString(16);
        } finally {
            is.close();
        }
        return result;
    }

    public String getTargetFileHash() {
        return targetFileHash;
    }

    public void setTargetFileHash(String targetFileHash) {
        this.targetFileHash = targetFileHash;
    }

    public boolean equals(Object o) {
        boolean result = false;
        if (o instanceof TargetDefinitionFile) {
            TargetDefinitionFile other = (TargetDefinitionFile) o;
            String h1 = getTargetFileHash();
            String h2 = other.getTargetFileHash();
            result = h1 != null && h1.equals(h2);
        }
        return result;
    }

}
