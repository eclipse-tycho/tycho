/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - cache target definition resolution result (bug 373806)
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
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

public final class TargetDefinitionFile implements TargetDefinition {

    private static XMLParser parser = new XMLParser();

    private Element dom;

    private Document document;

    private byte[] fileContentHash;

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

        /**
         * @deprecated Not for productive use. Breaks the
         *             {@link TargetDefinitionFile#equals(Object)} and
         *             {@link TargetDefinitionFile#hashCode()} implementations.
         */
        @Deprecated
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

        /**
         * @deprecated Not for productive use. Breaks the
         *             {@link TargetDefinitionFile#equals(Object)} and
         *             {@link TargetDefinitionFile#hashCode()} implementations.
         */
        @Deprecated
        public void setVersion(String version) {
            dom.setAttribute("version", version);
        }
    }

    private TargetDefinitionFile(File source) {
        try {
            this.fileContentHash = computeFileContentHash(source);

            FileInputStream input = new FileInputStream(source);
            try {
                this.document = parser.parse(new XMLIOSource(source));
                this.dom = document.getRootElement();
            } finally {
                input.close();
            }
        } catch (XMLParseException e) {
            throw new TargetDefinitionSyntaxException("Target definition is not well-formed XML: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new TargetDefinitionSyntaxException("I/O error while reading target definition file: "
                    + e.getMessage(), e);
        }
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

    public boolean hasIncludedBundles() {
        return dom.getChild("includeBundles") != null;
    }

    public static TargetDefinitionFile read(File file) throws IOException {
        return new TargetDefinitionFile(file);
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

    @Override
    public int hashCode() {
        return Arrays.hashCode(fileContentHash);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof TargetDefinitionFile))
            return false;

        TargetDefinitionFile other = (TargetDefinitionFile) obj;
        return Arrays.equals(fileContentHash, other.fileContentHash);
    }

    private static byte[] computeFileContentHash(File source) {
        byte[] digest;
        try {
            FileInputStream in = new FileInputStream(source);
            try {
                digest = computeMD5Digest(in);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error while reading \"" + source + "\": " + e.getMessage(), e);
        }
        return digest;
    }

    private static byte[] computeMD5Digest(FileInputStream in) throws IOException {
        MessageDigest digest = newMD5Digest();

        byte[] buffer = new byte[4 * 1024];
        while (in.read(buffer) > 0) {
            digest.update(buffer);
        }
        return digest.digest();
    }

    private static MessageDigest newMD5Digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
