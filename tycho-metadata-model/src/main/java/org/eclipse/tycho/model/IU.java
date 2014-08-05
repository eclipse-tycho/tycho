/*******************************************************************************
 * Copyright (c) 2015 Rapicorp, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Rapicorp, Inc. - initial API and implementation
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
import java.util.List;

import org.codehaus.plexus.util.IOUtil;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;
import de.pdark.decentxml.XMLWriter;

public class IU {
    public static final String P2_IU = "p2iu.xml";

    private static XMLParser parser = new XMLParser();

    private final Document document;

    private final Element iuDom;

    public IU(Document document, Element element) {
        this.document = document;
        this.iuDom = element;
    }

    public String getId() {
        return iuDom.getAttributeValue("id");
    }

    public String getVersion() {
        return iuDom.getAttributeValue("version");
    }

    public void setVersion(String version) {
        iuDom.setAttribute("version", version);
    }

    public List<Element> getProvidedCapabilites() {
        List<Element> provides = iuDom.getChildren("provides");
        if (provides == null || provides.size() == 0)
            return null;
        return provides.get(0).getChildren("provided");
    }

    public List<Element> getRequiredCapabilites() {
        List<Element> requires = iuDom.getChildren("requires");
        if (requires == null || requires.size() == 0)
            return null;
        return requires.get(0).getChildren("required");
    }

    public List<Element> getProperties() {
        List<Element> properties = iuDom.getChildren("properties");
        if (properties == null || properties.size() == 0)
            return null;
        return properties.get(0).getChildren("property");
    }

    public void addProperty(String name, String value) {
        Element properties = iuDom.getChild("properties");
        if (properties == null) {
            iuDom.addNode(new Element("properties"));
            properties = iuDom.getChild("properties");
        }
        Element elt = new Element("property");
        elt.setAttribute("name", name);
        elt.setAttribute("value", value);
        properties.addNode(elt);
    }

    public List<Element> getArtifacts() {
        Element artifacts = iuDom.getChild("artifacts");
        if (artifacts == null)
            return null;
        return artifacts.getChildren("artifact");
    }

    public static IU read(File file) throws IOException {
        FileInputStream is = new FileInputStream(file);
        try {

            Document iuDocument = parser.parse(new XMLIOSource(is));
            Element root = iuDocument.getChild("units");
            if (root == null)
                throw new RuntimeException("No ius found.");
            List<Element> ius = root.getChildren();
            if (ius.size() > 1)
                throw new RuntimeException(String.format("Too many IU found. Only one IU can be specified in "
                        + file.getAbsolutePath()));
            if (ius.size() == 0)
                throw new RuntimeException(String.format("No IU found in ", file.getAbsolutePath()));

            return new IU(iuDocument, ius.get(0));
        } finally {
            IOUtil.close(is);
        }
    }

    /**
     * Convenience method to load p2iu.xml file
     * 
     * @throws RuntimeException
     *             if iu descriptor can not be read or parsed.
     */
    public static IU loadIU(File location) {
        try {
            if (location.isDirectory()) {
                return IU.read(new File(location, IU.P2_IU));
            } else {
                throw new RuntimeException("Could not read iu descriptor at " + location.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read iu descriptor at " + location.getAbsolutePath(), e);
        }
    }

    public static void write(IU iu, File file) throws IOException {
        write(iu, file, "\t");
    }

    public static void write(IU iu, File file, String indent) throws IOException {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(file));

        Document document = iu.document;
        try {
            String enc = document.getEncoding() != null ? document.getEncoding() : "UTF-8";
            Writer w = new OutputStreamWriter(os, enc);
            XMLWriter xw = new XMLWriter(w);
            xw.setIndent(indent);
            try {
                document.toXML(xw);
            } finally {
                xw.flush();
            }
        } finally {
            IOUtil.close(os);
        }
    }

    public void addArtifact(String classifier, String id, String version) {
        Element artifacts = iuDom.getChild("artifacts");
        if (artifacts == null) {
            artifacts = new Element("artifacts");
            iuDom.addNode(artifacts);
        }
        Element newArtifact = new Element("artifact");
        newArtifact.addAttribute("classifier", classifier);
        newArtifact.addAttribute("id", id);
        newArtifact.addAttribute("version", version);

        artifacts.addNode(newArtifact);
    }

    public Element getSelfArtifact() {
        List<Element> artifacts = getArtifacts();
        if (artifacts == null)
            return null;
        for (Element artifact : artifacts) {
            if (getId().equals(artifact.getAttributeValue("id"))
                    && "binary".equals(artifact.getAttributeValue("classifier")))
                return artifact;
        }
        return null;
    }

    public Element getSelfCapability() {
        List<Element> providedCapabilities = getProvidedCapabilites();
        if (providedCapabilities == null)
            return null;
        for (Element capability : providedCapabilities) {

            if (getId().equals(capability.getAttributeValue("name"))
                    && "org.eclipse.equinox.p2.iu".equals(capability.getAttributeValue("namespace")))
                return capability;
        }
        return null;
    }

    public boolean hasP2IUProperty() {
        List<Element> props = getProperties();
        if (props == null)
            return false;
        for (Element prop : props) {
            if ("org.eclipse.equinox.p2.type.iu".equals(prop.getAttributeValue("name"))
                    && "true".equals(prop.getAttributeValue("value")))
                return true;
        }
        return false;
    }
}
