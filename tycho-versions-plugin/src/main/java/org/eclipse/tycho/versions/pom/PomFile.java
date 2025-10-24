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
 *    Bachmann electronic GmbH. - #472579 - Support setting the version for pomless builds
 *    Christoph Läubrich - Bug 550313 - tycho-versions-plugin uses hard-coded polyglot file
 *    SAP SE - #3744 - ci-friendly version support
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

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;
import eu.maveniverse.domtrip.Node;
import eu.maveniverse.domtrip.Text;

import eu.maveniverse.domtrip.DomTripException;

import eu.maveniverse.domtrip.Serializer;

public class PomFile {

    public static final String POM_XML = "pom.xml";
    private static final String DEFAULT_XML_ENCODING = "UTF-8";

    private static Serializer serializer = new Serializer();

    private Document document;
    private Element project;

    /** The (raw) project version */
    private String version;
    /** The ${property}-resolved version, in case of ci-friendly versions */
    private String resolvedVersion;
    private final boolean preferExplicitProjectVersion;
    private final boolean isMutable;

    public PomFile(Document pom, boolean isMutable) {
        this.document = pom;
        this.isMutable = isMutable;
        this.project = document.root();

        this.version = this.getExplicitVersionFromXML();
        if (this.version == null) {
            this.version = this.getParentVersion();
            this.preferExplicitProjectVersion = false;
        } else {
            this.preferExplicitProjectVersion = this.version.equals(this.getParentVersion());
        }
    }

    public static PomFile read(File file, boolean isMutable) throws IOException {
        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            return read(is, isMutable);
        } catch (DomTripException xpe) {
            throw new DomTripException("This Pom " + file.getAbsolutePath() + " is in the Wrong Format", xpe);
        }
    }

    public static PomFile read(InputStream input, boolean isMutable) throws IOException {
        return new PomFile(Document.of(input), isMutable);
    }

    public static void write(PomFile pom, OutputStream out) throws IOException {
        String encoding = pom.document.encoding() != null ? pom.document.encoding() : DEFAULT_XML_ENCODING;
        Writer w = new OutputStreamWriter(out, encoding);
        XMLWriter xw = new XMLWriter(w);
        try {
            pom.setVersionInXML();
            pom.document.toXML(xw);
        } finally {
            xw.flush();
        }
    }

    public static void write(PomFile pom, File file) throws IOException {
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
            write(pom, os);
        }
    }

    private String getExplicitVersionFromXML() {
        return getElementValue("version");
    }

    private void setVersionInXML() {
        boolean writeProjectVersion = preferExplicitProjectVersion || !version.equals(getParentVersion());
        if (writeProjectVersion) {
            Element versionElement = project.getChild("version");
            if (versionElement == null) {
                versionElement = addEmptyVersionElementToXML(project);
            }
            versionElement.textContent(version);
        } else {
            removeVersionElementFromXML(project);
        }
    }

    private static Element addEmptyVersionElementToXML(Element project) {
        Element result = Element.of(project, "version");
        // TODO proper indentation
        project.addNode(Text.of("\n"));
        return result;
    }

    private static void removeVersionElementFromXML(Element project) {
        List<Node> elements = project.getNodes();
        for (Iterator<Node> iterator = elements.iterator(); iterator.hasNext();) {
            Node node = iterator.next();
            if (node instanceof Element element) {
                if ("version".equals(element.name())) {
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
        element.textContent(newVersion);
    }

    /**
     * Sets the version of the project.
     */
    public void setVersion(String version) {
        this.version = version;
        this.resolvedVersion = null;
    }

    /**
     * Returns the (effective) version of the project with properties resolved.
     */
    public String getVersion() {
        if (this.resolvedVersion == null) {
            this.resolvedVersion = PomUtil.expandProperties(version, getProperties());
        }
        return this.resolvedVersion;
    }

    /**
     * Returns the literal version of the project without any properties resolved.
     */
    public String getRawVersion() {
        return this.version;
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
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (Element modules : project.children("modules").toList()) {
            for (Element module : modules.children("module").toList()) {
                result.add(module.getTrimmedText());
            }
        }
        return new ArrayList<>(result);
    }

    public List<Profile> getProfiles() {
        ArrayList<Profile> result = new ArrayList<>();
        for (Element profiles : project.children("profiles").toList()) {
            for (Element profile : profiles.children("profile").toList()) {
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

    public boolean isMutable() {
        return isMutable;
    }
}
