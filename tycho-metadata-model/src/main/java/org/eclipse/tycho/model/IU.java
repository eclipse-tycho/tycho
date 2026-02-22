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
import java.util.ArrayList;
import java.util.List;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;

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

    private final Document document;

    private final Element iuDom;

    public IU(Document document, Element element) {
        this.document = document;
        this.iuDom = element;
    }

    public String getId() {
        return iuDom.attribute(ID);
    }

    public String getVersion() {
        return iuDom.attribute(VERSION);
    }

    public void setVersion(String version) {
        iuDom.attribute(VERSION, version);
    }

    public List<Element> getProvidedCapabilites() {
        List<Element> provides = iuDom.children(PROVIDES).toList();
        if (provides == null || provides.isEmpty())
            return null;
        return provides.get(0).children(PROVIDED).toList();
    }

    public List<Element> getSelfCapabilities() {
        List<Element> selfCapabilities = new ArrayList<>(1);
        List<Element> providedCapabilities = getProvidedCapabilites();
        if (providedCapabilities == null)
            return selfCapabilities;
        for (Element capability : providedCapabilities) {
            if (getId().equals(capability.attribute(NAME))
                    && P2_IU_NAMESPACE.equals(capability.attribute(NAMESPACE)))
                selfCapabilities.add(capability);
        }
        return selfCapabilities;
    }

    public void addSelfCapability() {
        Element provides = iuDom.child(PROVIDES).orElse(null);
        if (provides == null) {
            provides = Element.of(PROVIDES);
            iuDom.addNode(provides);
        }
        Element newCapability = Element.of(PROVIDED);
        newCapability.attribute(NAMESPACE, P2_IU_NAMESPACE);
        newCapability.attribute(NAME, getId());
        newCapability.attribute(VERSION, getVersion());

        provides.addNode(newCapability);
    }

    public List<Element> getRequiredCapabilites() {
        List<Element> requires = iuDom.children(REQUIRES).toList();
        if (requires == null || requires.isEmpty())
            return null;
        return requires.get(0).children(REQUIRED).toList();
    }

    public List<Element> getProperties() {
        List<Element> properties = iuDom.children(PROPERTIES).toList();
        if (properties == null || properties.isEmpty())
            return null;
        return properties.get(0).children(PROPERTY).toList();
    }

    public void addProperty(String name, String value) {
        Element properties = iuDom.child(PROPERTIES).orElse(null);
        if (properties == null) {
            iuDom.addNode(Element.of(PROPERTIES));
            properties = iuDom.child(PROPERTIES).orElse(null);
        }
        Element elt = Element.of(PROPERTY);
        elt.attribute(NAME, name);
        elt.attribute("value", value);
        properties.addNode(elt);
    }

    public List<Element> getArtifacts() {
        Element artifacts = iuDom.child(ARTIFACTS).orElse(null);
        if (artifacts == null)
            return null;
        return artifacts.children(ARTIFACT).toList();
    }

    public void addArtifact(String classifier, String id, String version) {
        Element artifacts = iuDom.child(ARTIFACTS).orElse(null);
        if (artifacts == null) {
            artifacts = Element.of(ARTIFACTS);
            iuDom.addNode(artifacts);
        }
        Element newArtifact = Element.of(ARTIFACT);
        newArtifact.attribute(CLASSIFIER, classifier);
        newArtifact.attribute(ID, id);
        newArtifact.attribute(VERSION, version);

        artifacts.addNode(newArtifact);
    }

    public Element getSelfArtifact() {
        List<Element> artifacts = getArtifacts();
        if (artifacts == null)
            return null;
        for (Element artifact : artifacts) {
            if (getId().equals(artifact.attribute(ID))
                    && "binary".equals(artifact.attribute(CLASSIFIER)))
                return artifact;
        }
        return null;
    }

    public static IU read(File file) throws IOException {
        try (FileInputStream is = new FileInputStream(file)) {
            Document iuDocument = Document.of(is);
            Element root = iuDocument.root();
            if (root == null || !UNIT.equals(root.name()))
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
            document.toXml(os);
        }
    }
}
