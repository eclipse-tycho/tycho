/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
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
 *                          - [Bug 569481] - Support for maven target location includeSource="true" attribute
 *                          - [Issue 189]  - Support multiple maven-dependencies for one target location
 *                          - [Issue 194]  - Support additional repositories defined in the maven-target location
 *                          - [Issue 401]  - Support nested targets
 *******************************************************************************/
package org.eclipse.tycho.p2.target.facade;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.tycho.core.shared.MavenArtifactRepositoryReference;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public final class TargetDefinitionFile implements TargetDefinition {

    private static final Map<URI, TargetDefinitionFile> FILE_CACHE = new ConcurrentHashMap<>();

    private final String origin;
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

    public class TargetRef implements TargetDefinition.TargetReferenceLocation {

        private String uri;
        private URI resolvedUri;

        public TargetRef(String uri) {
            this.uri = uri;
        }

        @Override
        public String getTypeDescription() {
            return "Target";
        }

        @Override
        public String getUri() {
            return uri;
        }

    }

    public class MavenLocation implements TargetDefinition.MavenGAVLocation {

        private Element dom;

        private Set<String> globalExcludes = new HashSet<>();

        public MavenLocation(Element dom) {
            this.dom = dom;
            List<Element> children = getChildren(dom, "exclude");
            for (Element element : children) {
                globalExcludes.add(element.getTextContent());
            }
        }

        @Override
        public String getTypeDescription() {
            return "Maven";
        }

        @Override
        public String getIncludeDependencyScope() {
            return dom.getAttribute("includeDependencyScope");
        }

        @Override
        public MissingManifestStrategy getMissingManifestStrategy() {
            String attributeValue = dom.getAttribute("missingManifest");
            if ("generate".equalsIgnoreCase(attributeValue)) {
                return MissingManifestStrategy.GENERATE;
            } else if ("ignore".equals(attributeValue)) {
                return MissingManifestStrategy.IGNORE;
            }
            return MissingManifestStrategy.ERROR;
        }

        @Override
        public boolean includeSource() {
            return Boolean.parseBoolean(dom.getAttribute("includeSource"));
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("MavenDependencyRoots = ");
            builder.append(getRoots());
            builder.append(", IncludeDependencyScope = ");
            builder.append(getIncludeDependencyScope());
            builder.append(", MissingManifestStrategy = ");
            builder.append(getMissingManifestStrategy());
            builder.append(", IncludeSource = ");
            builder.append(includeSource());
            return builder.toString();
        }

        @Override
        public Collection<BNDInstructions> getInstructions() {
            List<BNDInstructions> list = new ArrayList<>();
            for (Element element : getChildren(dom, "instructions")) {
                String reference = element.getAttribute("reference");
                String text = element.getTextContent();
                Properties properties = new Properties();
                try {
                    properties.load(new StringReader(text));
                } catch (IOException e) {
                    throw new TargetDefinitionSyntaxException("parsing instructions into properties failed", e);
                }
                list.add(new BNDInstructions() {

                    @Override
                    public String getReference() {
                        if (reference == null) {
                            return "";
                        }
                        return reference;
                    }

                    @Override
                    public Properties getInstructions() {
                        return properties;
                    }
                });
            }
            return list;
        }

        @Override
        public Collection<MavenDependency> getRoots() {
            for (Element dependencies : getChildren(dom, "dependencies")) {
                List<MavenDependency> roots = new ArrayList<>();
                for (Element dependency : getChildren(dependencies, "dependency")) {
                    roots.add(new MavenDependencyRoot(dependency, this));
                }
                return roots;
            }
            //backward compatibility for old format...
            return Collections.singleton(new MavenDependencyRoot(dom, this));
        }

        @Override
        public Collection<MavenArtifactRepositoryReference> getRepositoryReferences() {
            for (Element dependencies : getChildren(dom, "repositories")) {
                List<MavenArtifactRepositoryReference> list = new ArrayList<MavenArtifactRepositoryReference>();
                for (Element repository : getChildren(dependencies, "repository")) {
                    list.add(new MavenArtifactRepositoryReference() {

                        @Override
                        public String getId() {
                            return getTextFromChild(repository, "id",
                                    String.valueOf(System.identityHashCode(repository)));
                        }

                        @Override
                        public String getUrl() {
                            return getTextFromChild(repository, "url", null);
                        }

                    });
                }
                return list;
            }
            return Collections.emptyList();
        }

        @Override
        public Element getFeatureTemplate() {
            return getChild(dom, "feature");
        }

        @Override
        public DependencyDepth getIncludeDependencyDepth() {
            if (dom.getAttributeNode("includeDependencyDepth") == null) {
                //backward compat
                String scope = getIncludeDependencyScope();
                if (scope == null || scope.isBlank()) {
                    return DependencyDepth.NONE;
                } else {
                    return DependencyDepth.INFINITE;
                }
            }
            String attribute = dom.getAttribute("includeDependencyDepth");
            if ("NONE".equalsIgnoreCase(attribute)) {
                return DependencyDepth.NONE;
            } else if ("DIRECT".equalsIgnoreCase(attribute)) {
                return DependencyDepth.DIRECT;
            } else if ("INFINITE".equalsIgnoreCase(attribute)) {
                return DependencyDepth.INFINITE;
            }
            //safe default
            return DependencyDepth.NONE;
        }

    }

    private static final class MavenDependencyRoot implements MavenDependency {

        private Element dom;
        private MavenLocation parent;

        public MavenDependencyRoot(Element dom, MavenLocation parent) {
            this.dom = dom;
            this.parent = parent;
        }

        @Override
        public String getGroupId() {
            return getTextFromChild(dom, "groupId", null);
        }

        @Override
        public String getArtifactId() {
            return getTextFromChild(dom, "artifactId", null);
        }

        @Override
        public String getVersion() {
            return getTextFromChild(dom, "version", null);
        }

        @Override
        public String getArtifactType() {
            return getTextFromChild(dom, "type", "jar");
        }

        @Override
        public String getClassifier() {
            return getTextFromChild(dom, "classifier", "");
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
            return builder.toString();
        }

        @Override
        public boolean isIgnored(IArtifactFacade artifact) {
            return parent.globalExcludes.contains(getKey(artifact));
        }

    }

    private static String getTextFromChild(Element dom, String childName, String defaultValue) {
        for (Element element : getChildren(dom, childName)) {
            return element.getTextContent();
        }
        if (defaultValue != null) {
            return defaultValue;
        }
        throw new TargetDefinitionSyntaxException("Missing child element '" + childName + "'");
    }

    private static List<Element> getChildren(Element element, String tagName) {
        NodeList list = element.getChildNodes();

        int length = list.getLength();
        List<Node> nodes = IntStream.range(0, length).mapToObj(item -> list.item(item)).collect(Collectors.toList());
        return nodes.stream().filter(Element.class::isInstance).map(Element.class::cast).filter(e -> {
            return e.getNodeName().equals(tagName);
        }).collect(Collectors.toList());
    }

    private static Element getChild(Element element, String tagName) {
        List<Element> list = getChildren(element, tagName);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    private static String getKey(IArtifactFacade artifact) {
        if (artifact == null) {
            return "";
        }
        String key = artifact.getGroupId() + ":" + artifact.getArtifactId();
        String classifier = artifact.getClassifier();
        if (classifier != null && !classifier.isBlank()) {
            key += ":" + classifier;
        }
        key += ":" + artifact.getVersion();
        return key;
    }

    public class IULocation implements TargetDefinition.InstallableUnitLocation {
        private final Element dom;

        public IULocation(Element dom) {
            this.dom = dom;
        }

        @Override
        public List<? extends TargetDefinition.Unit> getUnits() {
            ArrayList<Unit> units = new ArrayList<>();
            for (Element unitDom : getChildren(dom, "unit")) {
                units.add(new Unit(unitDom));
            }
            return Collections.unmodifiableList(units);
        }

        @Override
        public List<? extends TargetDefinition.Repository> getRepositories() {
            return getRepositoryImpls();
        }

        public List<Repository> getRepositoryImpls() {
            final List<Element> repositoryNodes = getChildren(dom, "repository");

            final List<Repository> repositories = new ArrayList<>(repositoryNodes.size());
            for (Element node : repositoryNodes) {
                repositories.add(new Repository(node));
            }
            return repositories;
        }

        @Override
        public String getTypeDescription() {
            return dom.getAttribute("type");
        }

        @Override
        public IncludeMode getIncludeMode() {
            Attr attributeValue = dom.getAttributeNode("includeMode");
            if (attributeValue == null || "planner".equals(attributeValue.getTextContent())) {
                return IncludeMode.PLANNER;
            } else if ("slicer".equals(attributeValue.getTextContent())) {
                return IncludeMode.SLICER;
            }
            throw new TargetDefinitionSyntaxException(
                    "Invalid value for attribute 'includeMode': " + attributeValue + "");
        }

        @Override
        public boolean includeAllEnvironments() {
            return Boolean.parseBoolean(dom.getAttribute("includeAllPlatforms"));
        }

        @Override
        public boolean includeSource() {
            return Boolean.parseBoolean(dom.getAttribute("includeSource"));
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
            return dom.getAttribute("id");
        }

        @Override
        public URI getLocation() {
            try {
                return new URI(dom.getAttribute("location"));
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
            return dom.getAttribute("id");
        }

        @Override
        public String getVersion() {
            return dom.getAttribute("version");
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

    private TargetDefinitionFile(URI uri) throws TargetDefinitionSyntaxException {
        try {
            this.origin = uri.toASCIIString();
            try (DigestInputStream input = new DigestInputStream(uri.toURL().openStream(), newMD5Digest())) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                this.document = builder.parse(input);
                this.dom = document.getDocumentElement();
                this.fileContentHash = input.getMessageDigest().digest();
            } catch (ParserConfigurationException e) {
                throw new TargetDefinitionSyntaxException("No valid XML parser: " + e.getMessage(), e);
            } catch (SAXException e) {
                throw new TargetDefinitionSyntaxException("Target definition is not well-formed XML: " + e.getMessage(),
                        e);
            }
        } catch (IOException e) {
            throw new TargetDefinitionSyntaxException(
                    "I/O error while reading target definition file: " + e.getMessage(), e);
        }
    }

    @Override
    public List<? extends TargetDefinition.Location> getLocations() {
        ArrayList<TargetDefinition.Location> locations = new ArrayList<>();
        Element locationsDom = getChild(dom, "locations");
        if (locationsDom != null) {
            for (Element locationDom : getChildren(locationsDom, "location")) {
                String type = locationDom.getAttribute("type");
                if ("InstallableUnit".equals(type)) {
                    locations.add(new IULocation(locationDom));
                } else if ("Directory".equals(type)) {
                    locations.add(new DirectoryTargetLocation(locationDom.getAttribute("path")));
                } else if ("Profile".equals(type)) {
                    locations.add(new ProfileTargetPlatformLocation(locationDom.getAttribute("path")));
                } else if ("Feature".equals(type)) {
                    locations.add(new FeatureTargetPlatformLocation(locationDom.getAttribute("path"),
                            locationDom.getAttribute("id"), locationDom.getAttribute("version")));
                } else if ("Maven".equals(type)) {
                    locations.add(new MavenLocation(locationDom));
                } else if ("Target".equals(type)) {
                    locations.add(new TargetRef(locationDom.getAttribute("uri")));
                } else {
                    locations.add(new OtherLocation(type));
                }
            }
        }
        return Collections.unmodifiableList(locations);
    }

    @Override
    public boolean hasIncludedBundles() {
        return getChild(dom, "includeBundles") != null;
    }

    @Override
    public String getOrigin() {
        return origin;
    }

    public static TargetDefinitionFile read(File file) {
        return read(file.toURI());
    }

    public static TargetDefinitionFile read(URI uri) {
        try {
            return FILE_CACHE.computeIfAbsent(uri, key -> new TargetDefinitionFile(key));
        } catch (TargetDefinitionSyntaxException e) {
            throw new RuntimeException("Invalid syntax in target definition " + uri + ": " + e.getMessage(), e);
        }
    }

    public static void write(TargetDefinitionFile target, File file) throws IOException {
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            try {
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(target.document);
                StreamResult result = new StreamResult(new FileOutputStream(file));
                transformer.transform(source, result);
            } catch (TransformerException e) {
                throw new IOException(e);
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

    private static MessageDigest newMD5Digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getTargetEE() {
        Element targetJRE = getChild(dom, "targetJRE");
        if (targetJRE != null) {
            Attr path = targetJRE.getAttributeNode("path");
            if (path != null) {
                String pathValue = path.getValue();
                return pathValue.substring(pathValue.lastIndexOf('/') + 1);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "TargetDefinitionFile[" + origin + "]";
    }

}
