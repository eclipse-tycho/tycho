/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP SE - cache target definition resolution result (bug 373806)
 *    Christoph Läubrich    - [Bug 538144] - support other target locations (Directory, Feature, Installations)
 *                          - [Bug 533747] - Target file is read and parsed over and over again
 *                          - [Bug 568729] - Support new "Maven" Target location
 *******************************************************************************/
package org.eclipse.tycho.core.ee;

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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionSyntaxException;

import de.pdark.decentxml.Attribute;
import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParseException;
import de.pdark.decentxml.XMLParser;
import de.pdark.decentxml.XMLWriter;

public final class TargetDefinitionFile implements TargetDefinition {

    private static final XMLParser PARSER = new XMLParser();

    private static final Map<String, TargetDefinitionFile> FILE_CACHE = new ConcurrentHashMap<>();

    private final File origin;
    private final byte[] fileContentHash;

    private final Element dom;
    private final Document document;

    private abstract class AbstractPathLocation implements TargetDefinition.PathLocation {
        private String path;

        public AbstractPathLocation(String path) {
            this.path = path;
        }

        @Override
        public String getPath() {
            return path;
        }
    }

    public class DirectoryTargetLocation extends AbstractPathLocation implements TargetDefinition.DirectoryLocation {

        public DirectoryTargetLocation(String path) {
            super(path);
        }

        @Override
        public String getTypeDescription() {
            return "Directory";
        }

    }

    public class ProfileTargetPlatformLocation extends AbstractPathLocation
            implements TargetDefinition.ProfileLocation {

        public ProfileTargetPlatformLocation(String path) {
            super(path);
        }

        @Override
        public String getTypeDescription() {
            return "Profile";
        }

    }

    public class FeatureTargetPlatformLocation extends AbstractPathLocation
            implements TargetDefinition.FeaturesLocation {

        private final String feature;
        private final String version;

        public FeatureTargetPlatformLocation(String path, String feature, String version) {
            super(path);
            this.feature = feature;
            this.version = version;
        }

        @Override
        public String getTypeDescription() {
            return "Feature";
        }

        @Override
        public String getId() {
            return feature;
        }

        @Override
        public String getVersion() {
            return version;
        }

    }

    public class MavenLocation implements TargetDefinition.MavenGAVLocation {

        private Element dom;

        public MavenLocation(Element dom) {
            this.dom = dom;
        }

        @Override
        public String getTypeDescription() {
            return "Maven";
        }

        @Override
        public String getIncludeDependencyScope() {
            return dom.getAttributeValue("includeDependencyScope");
        }

        @Override
        public MissingManifestStrategy getMissingManifestStrategy() {
            String attributeValue = dom.getAttributeValue("missingManifest");
            if ("generate".equalsIgnoreCase(attributeValue)) {
                return MissingManifestStrategy.GENERATE;
            } else if ("ignore".equals(attributeValue)) {
                return MissingManifestStrategy.IGNORE;
            }
            return MissingManifestStrategy.ERROR;
        }

        @Override
        public String getGroupId() {
            return getTextFromChild("groupId", null);
        }

        @Override
        public String getArtifactId() {
            return getTextFromChild("artifactId", null);
        }

        @Override
        public String getVersion() {
            return getTextFromChild("version", null);
        }

        @Override
        public String getArtifactType() {
            return getTextFromChild("type", "jar");
        }

        @Override
        public String getClassifier() {
            return getTextFromChild("classifier", "");
        }

        private String getTextFromChild(String childName, String defaultValue) {
            for (Element element : dom.getChildren(childName)) {
                return element.getNormalizedText();
            }
            if (defaultValue != null) {
                return defaultValue;
            }
            throw new TargetDefinitionSyntaxException("Missing child element '" + childName + "'");
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("GroupId = ");
            builder.append(getGroupId());
            builder.append(", ArtifactId = ");
            builder.append(getArtifactId());
            builder.append(", Version = ");
            builder.append(getVersion());
            builder.append(", ArtifactType = ");
            builder.append(getArtifactType());
            builder.append(", IncludeDependencyScope = ");
            builder.append(getIncludeDependencyScope());
            builder.append(", MissingManifestStrategy = ");
            builder.append(getMissingManifestStrategy());
            return builder.toString();
        }

    }

    public class IULocation implements TargetDefinition.InstallableUnitLocation {
        private final Element dom;

        public IULocation(Element dom) {
            this.dom = dom;
        }

        @Override
        public List<? extends TargetDefinition.Unit> getUnits() {
            ArrayList<Unit> units = new ArrayList<>();
            for (Element unitDom : dom.getChildren("unit")) {
                units.add(new Unit(unitDom));
            }
            return Collections.unmodifiableList(units);
        }

        @Override
        public List<? extends TargetDefinition.Repository> getRepositories() {
            return getRepositoryImpls();
        }

        public List<Repository> getRepositoryImpls() {
            final List<Element> repositoryNodes = dom.getChildren("repository");

            final List<Repository> repositories = new ArrayList<>(repositoryNodes.size());
            for (Element node : repositoryNodes) {
                repositories.add(new Repository(node));
            }
            return repositories;
        }

        @Override
        public String getTypeDescription() {
            return dom.getAttributeValue("type");
        }

        @Override
        public IncludeMode getIncludeMode() {
            String attributeValue = dom.getAttributeValue("includeMode");
            if ("slicer".equals(attributeValue)) {
                return IncludeMode.SLICER;
            } else if ("planner".equals(attributeValue) || attributeValue == null) {
                return IncludeMode.PLANNER;
            }
            throw new TargetDefinitionSyntaxException(
                    "Invalid value for attribute 'includeMode': " + attributeValue + "");
        }

        @Override
        public boolean includeAllEnvironments() {
            return Boolean.parseBoolean(dom.getAttributeValue("includeAllPlatforms"));
        }

        @Override
        public boolean includeSource() {
            return Boolean.parseBoolean(dom.getAttributeValue("includeSource"));
        }
    }

    public static class OtherLocation implements Location {
        private final String description;

        public OtherLocation(String description) {
            this.description = description;
        }

        @Override
        public String getTypeDescription() {
            return description;
        }
    }

    public static final class Repository implements TargetDefinition.Repository {
        private final Element dom;

        public Repository(Element dom) {
            this.dom = dom;
        }

        @Override
        public String getId() {
            // this is Maven specific, used to match credentials and mirrors
            return dom.getAttributeValue("id");
        }

        @Override
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

        @Override
        public String getId() {
            return dom.getAttributeValue("id");
        }

        @Override
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

    private TargetDefinitionFile(File source) throws TargetDefinitionSyntaxException {
        try {
            this.origin = source;
            this.fileContentHash = computeFileContentHash(source);

            try (FileInputStream input = new FileInputStream(source)) {
                this.document = PARSER.parse(new XMLIOSource(source));
                this.dom = document.getRootElement();
            }
        } catch (XMLParseException e) {
            throw new TargetDefinitionSyntaxException("Target definition is not well-formed XML: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new TargetDefinitionSyntaxException(
                    "I/O error while reading target definition file: " + e.getMessage(), e);
        }
    }

    @Override
    public List<? extends TargetDefinition.Location> getLocations() {
        ArrayList<TargetDefinition.Location> locations = new ArrayList<>();
        Element locationsDom = dom.getChild("locations");
        if (locationsDom != null) {
            for (Element locationDom : locationsDom.getChildren("location")) {
                String type = locationDom.getAttributeValue("type");
                if ("InstallableUnit".equals(type)) {
                    locations.add(new IULocation(locationDom));
                } else if ("Directory".equals(type)) {
                    locations.add(new DirectoryTargetLocation(locationDom.getAttributeValue("path")));
                } else if ("Profile".equals(type)) {
                    locations.add(new ProfileTargetPlatformLocation(locationDom.getAttributeValue("path")));
                } else if ("Feature".equals(type)) {
                    locations.add(new FeatureTargetPlatformLocation(locationDom.getAttributeValue("path"),
                            locationDom.getAttributeValue("id"), locationDom.getAttributeValue("version")));
                } else if ("Maven".equals(type)) {
                    locations.add(new MavenLocation(locationDom));
                } else {
                    locations.add(new OtherLocation(type));
                }
            }
        }
        return Collections.unmodifiableList(locations);
    }

    @Override
    public boolean hasIncludedBundles() {
        return dom.getChild("includeBundles") != null;
    }

    @Override
    public String getOrigin() {
        return origin.getAbsolutePath();
    }

    public static TargetDefinitionFile read(File file) {
        try {
            return FILE_CACHE.computeIfAbsent(file.getAbsolutePath(), key -> new TargetDefinitionFile(file));
        } catch (TargetDefinitionSyntaxException e) {
            throw new RuntimeException("Invalid syntax in target definition " + file + ": " + e.getMessage(), e);
        }
    }

    public static void write(TargetDefinitionFile target, File file) throws IOException {
        Document document = target.document;
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
            String enc = document.getEncoding() != null ? document.getEncoding() : "UTF-8";
            Writer w = new OutputStreamWriter(os, enc);
            XMLWriter xw = new XMLWriter(w);
            try {
                document.toXML(xw);
            } finally {
                xw.flush();
            }
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
            try (FileInputStream in = new FileInputStream(source)) {
                digest = computeMD5Digest(in);
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error while reading \"" + source + "\": " + e.getMessage(), e);
        }
        return digest;
    }

    private static byte[] computeMD5Digest(FileInputStream in) throws IOException {
        MessageDigest digest = newMD5Digest();

        byte[] buffer = new byte[4 * 1024];
        int read;
        while ((read = in.read(buffer)) > -1) {
            digest.update(buffer, 0, read);
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

    @Override
    public String getTargetEE() {
        Element targetJRE = dom.getChild("targetJRE");
        if (targetJRE != null) {
            Attribute path = targetJRE.getAttribute("path");
            if (path != null) {
                String pathValue = path.getValue();
                String ee = pathValue.substring(pathValue.lastIndexOf('/') + 1);
                return ee;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "TargetDefinitionFile[" + origin + "]";
    }

}
