/*******************************************************************************
 * Copyright (c) 2015, 2020 Rapicorp, Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import java.util.ArrayList;
import java.util.List;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;
import de.pdark.decentxml.XMLWriter;

public class IU {
    public static final String SOURCE_FILE_NAME = "p2iu.xml";

    private static final String UNIT = "unit";
    public static final String ID = "id";
    public static final String VERSION = "version";

    public static final String NAMESPACE = "namespace";
    public static final String NAME = "name";

    private static final String PROPERTIES = "properties";
    private static final String PROPERTY = "property";

    private static final String REQUIRES = "requires";
    private static final String REQUIRED = "required";
    public static final String RANGE = "range";

    private static final String ARTIFACTS = "artifacts";
    private static final String CLASSIFIER = "classifier";
    private static final String ARTIFACT = "artifact";

    private static final String PROVIDES = "provides";
    private static final String PROVIDED = "provided";

    public static final String P2_IU_NAMESPACE = "org.eclipse.equinox.p2.iu";

    private static XMLParser parser = new XMLParser();

    private final Document document;

    private final Element iuDom;

    public IU(Document document, Element element) {
        this.document = document;
        this.iuDom = element;
    }

    public String getId() {
        return iuDom.getAttributeValue(ID);
    }

    public String getVersion() {
        return iuDom.getAttributeValue(VERSION);
    }

    public void setVersion(String version) {
        iuDom.setAttribute(VERSION, version);
    }

    public List<Element> getProvidedCapabilites() {
        List<Element> provides = iuDom.getChildren(PROVIDES);
        if (provides == null || provides.isEmpty())
            return null;
        return provides.get(0).getChildren(PROVIDED);
    }

    public List<Element> getSelfCapabilities() {
        List<Element> selfCapabilities = new ArrayList<>(1);
        List<Element> providedCapabilities = getProvidedCapabilites();
        if (providedCapabilities == null)
            return selfCapabilities;
        for (Element capability : providedCapabilities) {
            if (getId().equals(capability.getAttributeValue(NAME))
                    && P2_IU_NAMESPACE.equals(capability.getAttributeValue(NAMESPACE)))
                selfCapabilities.add(capability);
        }
        return selfCapabilities;
    }

    public void addSelfCapability() {
        Element provides = iuDom.getChild(PROVIDES);
        if (provides == null) {
            provides = new Element(PROVIDES);
            iuDom.addNode(provides);
        }
        Element newCapability = new Element(PROVIDED);
        newCapability.addAttribute(NAMESPACE, P2_IU_NAMESPACE);
        newCapability.addAttribute(NAME, getId());
        newCapability.addAttribute(VERSION, getVersion());

        provides.addNode(newCapability);
    }

    public List<Element> getRequiredCapabilites() {
        List<Element> requires = iuDom.getChildren(REQUIRES);
        if (requires == null || requires.isEmpty())
            return null;
        return requires.get(0).getChildren(REQUIRED);
    }

    public List<Element> getProperties() {
        List<Element> properties = iuDom.getChildren(PROPERTIES);
        if (properties == null || properties.isEmpty())
            return null;
        return properties.get(0).getChildren(PROPERTY);
    }

    public void addProperty(String name, String value) {
        Element properties = iuDom.getChild(PROPERTIES);
        if (properties == null) {
            iuDom.addNode(new Element(PROPERTIES));
            properties = iuDom.getChild(PROPERTIES);
        }
        Element elt = new Element(PROPERTY);
        elt.setAttribute(NAME, name);
        elt.setAttribute("value", value);
        properties.addNode(elt);
    }

    public List<Element> getArtifacts() {
        Element artifacts = iuDom.getChild(ARTIFACTS);
        if (artifacts == null)
            return null;
        return artifacts.getChildren(ARTIFACT);
    }

    public void addArtifact(String classifier, String id, String version) {
        Element artifacts = iuDom.getChild(ARTIFACTS);
        if (artifacts == null) {
            artifacts = new Element(ARTIFACTS);
            iuDom.addNode(artifacts);
        }
        Element newArtifact = new Element(ARTIFACT);
        newArtifact.addAttribute(CLASSIFIER, classifier);
        newArtifact.addAttribute(ID, id);
        newArtifact.addAttribute(VERSION, version);

        artifacts.addNode(newArtifact);
    }

    public Element getSelfArtifact() {
        List<Element> artifacts = getArtifacts();
        if (artifacts == null)
            return null;
        for (Element artifact : artifacts) {
            if (getId().equals(artifact.getAttributeValue(ID))
                    && "binary".equals(artifact.getAttributeValue(CLASSIFIER)))
                return artifact;
        }
        return null;
    }

    public static IU read(File file) throws IOException {
        try (FileInputStream is = new FileInputStream(file)) {
            Document iuDocument = parser.parse(new XMLIOSource(is));
            Element root = iuDocument.getChild(UNIT);
            if (root == null)
                throw new RuntimeException("No iu found.");

            IU result = new IU(iuDocument, root);
            if (result.getId() == null)
                throw new RuntimeException(
                        String.format("The IU defined in %s is missing an id.", file.getAbsolutePath()));
            if (result.getVersion() == null)
                throw new RuntimeException(
                        String.format("The IU defined in %s is missing a version.", file.getAbsolutePath()));
            return result;
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
                return IU.read(new File(location, IU.SOURCE_FILE_NAME));
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
        Document document = iu.document;
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
            String enc = document.getEncoding() != null ? document.getEncoding() : "UTF-8";
            Writer w = new OutputStreamWriter(os, enc);
            XMLWriter xw = new XMLWriter(w);
            xw.setIndent(indent);
            try {
                document.toXML(xw);
            } finally {
                xw.flush();
            }
        }
    }
}
