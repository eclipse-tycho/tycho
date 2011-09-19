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
import java.util.LinkedHashSet;
import java.util.List;

import org.codehaus.plexus.util.IOUtil;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;
import de.pdark.decentxml.XMLWriter;

public class MutablePomFile {
    private static XMLParser parser = new XMLParser();

    private Document document;

    private Element project;

    public MutablePomFile(Document pom) {
        this.document = pom;
        this.project = document.getRootElement();
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

    public void setVersion(String version) {
        Element element = project.getChild("version");
        if (element == null) {
            element = new Element(project, "version");
        }
        element.setText(version);
    }

    public void setParentVersion(String newVersion) {
        Element element = project.getChild("parent/version");
        if (element == null) {
            throw new IllegalArgumentException("No parent/version");
        }

        element.setText(newVersion);
    }

    public String getVersion() {
        Element element = project.getChild("version");
        return element != null ? element.getText() : null;
    }

    public String getEffectiveVersion() {
        String version = getVersion();
        if (version == null) {
            version = getParentVersion();
        }
        return version;
    }

    public String getPackaging() {
        Element packaging = project.getChild("packaging");
        return packaging != null ? packaging.getText() : "jar";
    }

    public String getParentVersion() {
        return getParent().getVersion();
    }

    public String getGroupId() {
        Element element = project.getChild("groupId");
        return element != null ? element.getText() : null;
    }

    public String getEffectiveGroupId() {
        String groupId = getGroupId();
        if (groupId == null) {
            groupId = getParent().getGroupId();
        }
        return groupId;
    }

    public String getArtifactId() {
        Element element = project.getChild("artifactId");
        return element != null ? element.getText() : null;
    }

    public GAV getParent() {
        Element element = project.getChild("parent");
        return element != null ? new GAV(element) : null;
    }

    public List<String> getModules() {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (Element modules : project.getChildren("modules")) {
            for (Element module : modules.getChildren("module")) {
                result.add(module.getText());
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

        Element dependencyManagement = project.getChild("dependencyManagement");

        if (dependencyManagement == null)
            return null;

        return new DependencyManagement(dependencyManagement);
    }

    public List<GAV> getDependencies() {
        ArrayList<GAV> result = new ArrayList<GAV>();

        Element dependencies = project.getChild("dependencies");

        if (dependencies != null) {
            for (Element dependency : dependencies.getChildren("dependency"))
                result.add(new GAV(dependency));
        }

        return result;

    }
}
