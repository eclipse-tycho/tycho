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
package org.eclipse.tycho.versions.pom;

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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.codehaus.plexus.util.IOUtil;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.Node;
import de.pdark.decentxml.Text;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;
import de.pdark.decentxml.XMLWriter;

public class MutablePomFile {

    public static final String POM_XML = "pom.xml";

    private static XMLParser parser = new XMLParser();

    private Document document;
    private Element project;

    private String version;

    public MutablePomFile(Document pom) {
        this.document = pom;
        this.project = document.getRootElement();

        this.version = this.getExplicitVersionFromXML();
        if (this.version == null) {
            this.version = this.getParentVersion();
        }
    }

    public static MutablePomFile read(File file) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        try {
            return read(is);
        } finally {
            IOUtil.close(is);
        }
    }

    public static MutablePomFile read(InputStream input) throws IOException {
        return new MutablePomFile(parser.parse(new XMLIOSource(input)));
    }

    public static void write(MutablePomFile pom, OutputStream out) throws IOException {
        Writer w = pom.document.getEncoding() != null ? new OutputStreamWriter(out, pom.document.getEncoding())
                : new OutputStreamWriter(out);
        XMLWriter xw = new XMLWriter(w);
        try {
            pom.setVersionInXML();
            pom.document.toXML(xw);
        } finally {
            xw.flush();
        }
    }

    public static void write(MutablePomFile pom, File file) throws IOException {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
        try {
            write(pom, os);
        } finally {
            IOUtil.close(os);
        }
    }

    private String getExplicitVersionFromXML() {
        return getElementValue("version");
    }

    private void setVersionInXML() {
        boolean needsExplicitVersion = !version.equals(getParentVersion());
        if (needsExplicitVersion) {
            Element versionElement = project.getChild("version");
            if (versionElement == null) {
                versionElement = addEmptyVersionElementToXML(project);
            }
            versionElement.setText(version);
        } else {
            removeVersionElementFromXML(project);
        }
    }

    private static Element addEmptyVersionElementToXML(Element project) {
        Element result = new Element(project, "version");
        // TODO proper indentation
        project.addNode(new Text("\n"));
        return result;
    }

    private static void removeVersionElementFromXML(Element project) {
        List<Node> elements = project.getNodes();
        for (Iterator<Node> iterator = elements.iterator(); iterator.hasNext();) {
            Node node = iterator.next();
            if (node instanceof Element) {
                if ("version".equals(((Element) node).getName())) {
                    iterator.remove();

                    // also return newline after the element
                    if (iterator.hasNext() && iterator.next() instanceof Text) {
                        iterator.remove();
                    }
                    return;
                }
            }
        }
    }

    /**
     * Sets the version in the parent POM declaration. This never affects the (effective) version of
     * the project itself.
     * 
     * @see #setVersion(String)
     */
    public void setParentVersion(String newVersion) {
        Element element = project.getChild("parent/version");
        if (element == null) {
            throw new IllegalArgumentException("No parent/version");
        }
        element.setText(newVersion);
    }

    /**
     * Sets the version of the project.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns the (effective) version of the project.
     */
    public String getVersion() {
        return version;
    }

    public String getPackaging() {
        String packaging = getElementValue("packaging");
        return packaging != null ? packaging : "jar";
    }

    public String getParentVersion() {
        GAV parent = getParent();
        return parent != null ? parent.getVersion() : null;
    }

    private String getExplicitGroupId() {
        return getElementValue("groupId");
    }

    /**
     * Returns the (effective) groupId of the project.
     */
    public String getGroupId() {
        String groupId = getExplicitGroupId();
        if (groupId == null) {
            groupId = getParent().getGroupId();
        }
        return groupId;
    }

    public String getArtifactId() {
        return getElementValue("artifactId");
    }

    public GAV getParent() {
        Element element = project.getChild("parent");
        return element != null ? new GAV(element) : null;
    }

    public List<String> getModules() {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (Element modules : project.getChildren("modules")) {
            for (Element module : modules.getChildren("module")) {
                result.add(module.getTrimmedText());
            }
        }
        return new ArrayList<String>(result);
    }

    public List<Profile> getProfiles() {
        ArrayList<Profile> result = new ArrayList<Profile>();
        for (Element profiles : project.getChildren("profiles")) {
            for (Element profile : profiles.getChildren("profile")) {
                result.add(new Profile(profile));
            }
        }
        return result;
    }

    public DependencyManagement getDependencyManagement() {
        return DependencyManagement.getDependencyManagement(project);
    }

    public List<GAV> getDependencies() {
        return Dependencies.getDependencies(project);
    }

    public Build getBuild() {
        return Build.getBuild(project);
    }

    public List<Property> getProperties() {
        return Property.getProperties(project);
    }

    private String getElementValue(String name) {
        Element child = project.getChild(name);
        return child != null ? child.getTrimmedText() : null;
    }
}
