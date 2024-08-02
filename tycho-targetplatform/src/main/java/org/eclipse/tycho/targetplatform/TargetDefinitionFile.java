/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
package org.eclipse.tycho.targetplatform;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.MavenArtifactRepositoryReference;
import org.eclipse.tycho.targetplatform.TargetDefinition.MavenGAVLocation.DependencyDepth;
import org.eclipse.tycho.targetplatform.TargetDefinition.MavenGAVLocation.MissingManifestStrategy;
import org.osgi.resource.Requirement;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.resource.CapReqBuilder;

public final class TargetDefinitionFile implements TargetDefinition {

	public static final String ELEMENT_LOCATIONS = "locations";
	private static final Map<URI, TargetDefinitionFile> FILE_CACHE = new ConcurrentHashMap<>();
    //just for information purpose
    private final String origin;

    private List<? extends Location> locations;

    private boolean hasIncludeBundles;

    private String targetEE;
	public static final String FILE_EXTENSION = ".target";
	public static final String APPLICATION_TARGET = "application/target";

    private abstract static class AbstractPathLocation implements TargetDefinition.PathLocation {
        private String path;

        public AbstractPathLocation(String path) {
            this.path = path;
        }

        @Override
        public String getPath() {
            return path;
        }
    }

    private static class DirectoryTargetLocation extends AbstractPathLocation
            implements TargetDefinition.DirectoryLocation {

        public DirectoryTargetLocation(String path) {
            super(path);
        }

        @Override
        public String getTypeDescription() {
            return "Directory";
        }

    }

    private static class ProfileTargetPlatformLocation extends AbstractPathLocation
            implements TargetDefinition.ProfileLocation {

        public ProfileTargetPlatformLocation(String path) {
            super(path);
        }

        @Override
        public String getTypeDescription() {
            return "Profile";
        }

    }

    private static class FeatureTargetPlatformLocation extends AbstractPathLocation
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

    private static class TargetRef implements TargetDefinition.TargetReferenceLocation {

        private String uri;

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

	private static final class OSGIRepositoryLocation implements TargetDefinition.RepositoryLocation {

		private String uri;
		private Collection<Requirement> requirements;

		public OSGIRepositoryLocation(String uri, Collection<Requirement> requirements) {
			this.uri = uri;
			this.requirements = requirements;
		}

		@Override
		public String getUri() {
			return uri;
		}

		@Override
		public Collection<Requirement> getRequirements() {
			return requirements;
		}

	}

    private static class MavenLocation implements TargetDefinition.MavenGAVLocation {

        private final Collection<String> includeDependencyScopes;
        private final MissingManifestStrategy manifestStrategy;
        private final boolean includeSource;
        private final Collection<BNDInstructions> instructions;
        private final Collection<MavenDependency> roots;
        private final DependencyDepth dependencyDepth;
        private final Collection<MavenArtifactRepositoryReference> repositoryReferences;
        private final Element featureTemplate;
		private String label;

        public MavenLocation(Collection<MavenDependency> roots, Collection<String> includeDependencyScopes,
                MissingManifestStrategy manifestStrategy, boolean includeSource,
                Collection<BNDInstructions> instructions, DependencyDepth dependencyDepth,
				Collection<MavenArtifactRepositoryReference> repositoryReferences, Element featureTemplate,
				String label) {
            this.roots = roots;
            this.includeDependencyScopes = includeDependencyScopes;
            this.manifestStrategy = manifestStrategy;
            this.includeSource = includeSource;
            this.instructions = instructions;
            this.dependencyDepth = dependencyDepth;
            this.repositoryReferences = repositoryReferences;
			this.label = label;
            this.featureTemplate = featureTemplate == null ? null : (Element) featureTemplate.cloneNode(true);
        }

        @Override
		public Collection<String> getIncludeDependencyScopes() {
            return includeDependencyScopes;
        }

        @Override
        public MissingManifestStrategy getMissingManifestStrategy() {
            return manifestStrategy;
        }

        @Override
        public boolean includeSource() {
            return includeSource;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("MavenDependencyRoots = ");
            builder.append(getRoots());
            builder.append(", IncludeDependencyScope = ");
            builder.append(getIncludeDependencyScopes());
            builder.append(", MissingManifestStrategy = ");
            builder.append(getMissingManifestStrategy());
            builder.append(", IncludeSource = ");
            builder.append(includeSource());
            return builder.toString();
        }

        @Override
        public Collection<BNDInstructions> getInstructions() {
            return instructions;
        }

        @Override
        public Collection<MavenDependency> getRoots() {
            return roots;
        }

        @Override
        public Collection<MavenArtifactRepositoryReference> getRepositoryReferences() {
            return repositoryReferences;
        }

        @Override
        public Element getFeatureTemplate() {
            return featureTemplate;
        }

        @Override
        public DependencyDepth getIncludeDependencyDepth() {
            return dependencyDepth;
        }

		@Override
		public String getLabel() {
			if (label != null && !label.isBlank()) {
				return label;
			}
			if (featureTemplate != null) {
				String featureLabel = featureTemplate.getAttribute("label");
				if (featureLabel != null && !featureLabel.isBlank()) {
					return featureLabel;
				}
				String featureId = featureTemplate.getAttribute("id");
				if (featureId != null && !featureId.isBlank()) {
					return featureId;
				}
			}
			if (roots.size() == 1) {
				MavenDependency dependency = roots.iterator().next();
				return MessageFormat.format("{0}:{1} ({2})", dependency.getGroupId(), dependency.getArtifactId(),
						dependency.getVersion());
			} else {
				return MessageFormat.format("{0} Maven Dependencies", roots.size());
			}
		}

    }

    private static final class MavenDependencyRoot implements MavenDependency {

        private final Set<String> globalExcludes;
        private final String groupId;
        private final String artifactId;
        private final String version;
        private final String classifier;
        private final String type;

        public MavenDependencyRoot(String groupId, String artifactId, String version, String classifier, String type,
                Set<String> globalExcludes) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.classifier = classifier;
            this.type = type;
            this.globalExcludes = globalExcludes;
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

        @Override
        public String getGroupId() {
            return groupId;
        }

        @Override
        public String getArtifactId() {
            return artifactId;
        }

        @Override
        public String getVersion() {
            return version;
        }

        @Override
        public String getArtifactType() {
            return type;
        }

        @Override
        public String getClassifier() {
            return classifier;
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
            return globalExcludes.contains(getKey(artifact));
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
        List<Node> nodes = IntStream.range(0, length).mapToObj(list::item).toList();
        return nodes.stream().filter(Element.class::isInstance).map(Element.class::cast)
                .filter(e -> e.getNodeName().equals(tagName)).toList();
    }

    private static Element getChild(Element element, String tagName) {
        List<Element> list = getChildren(element, tagName);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    private static class IULocation implements TargetDefinition.InstallableUnitLocation {

        private final List<Unit> units;
        private final List<Repository> repositories;
        private final IncludeMode includeMode;
        private final boolean includeAllEnvironments;
        private final boolean includeSource;
        private final boolean includeConfigurePhase;
        private final FollowRepositoryReferences followRepositoryReferences;

        IULocation(List<Unit> units, List<Repository> repositories, IncludeMode includeMode,
                boolean includeAllEnvironments, boolean includeSource, boolean includeConfigurePhase,
                FollowRepositoryReferences followRepositoryReferences) {
            this.units = units;
            this.repositories = repositories;
            this.includeMode = includeMode;
            this.includeAllEnvironments = includeAllEnvironments;
            this.includeSource = includeSource;
            this.includeConfigurePhase = includeConfigurePhase;
            this.followRepositoryReferences = followRepositoryReferences;
        }

        @Override
        public List<? extends TargetDefinition.Unit> getUnits() {
            return units;
        }

        @Override
        public List<? extends TargetDefinition.Repository> getRepositories() {
            return repositories;
        }

        @Override
        public IncludeMode getIncludeMode() {
            return includeMode;
        }

        @Override
        public boolean includeAllEnvironments() {
            return includeAllEnvironments;
        }

        @Override
        public boolean includeSource() {
            return includeSource;
        }

        @Override
        public boolean includeConfigurePhase() {
            return includeConfigurePhase;
        }

        @Override
        public FollowRepositoryReferences followRepositoryReferences() {
            return followRepositoryReferences;
        }
    }

    private static class OtherLocation implements Location {
        private final String description;

        OtherLocation(String description) {
            this.description = description;
        }

        @Override
        public String getTypeDescription() {
            return description;
        }
    }

    private static final class Repository implements TargetDefinition.Repository {

        private final String id;
        private final String uri;

        Repository(String id, String uri) {
            this.id = id;
            this.uri = uri;
        }

        @Override
        public String getId() {
            // this is Maven specific, used to match credentials and mirrors
            return id;
        }

        @Override
        public String getLocation() {
            return uri;
        }

    }

    private static class Unit implements TargetDefinition.Unit {

        private String id;
        private String version;

        Unit(String id, String version) {
            this.id = id;
            this.version = version;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getVersion() {
            return version;
        }

		@Override
		public String toString() {
			return "Unit [id=" + id + ", version=" + version + "]";
		}

    }

    private TargetDefinitionFile(Document document, String origin) throws TargetDefinitionSyntaxException {
        this.origin = origin;
        Element dom = document.getDocumentElement();
        locations = parseLocations(dom);
        hasIncludeBundles = getChild(dom, "includeBundles") != null;
        targetEE = parseTargetEE(dom);
    }

    public static Document parseDocument(InputStream input)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(input);
    }

    public static void writeDocument(Document document, OutputStream outputStream) throws IOException {
        try (OutputStream os = new BufferedOutputStream(outputStream)) {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            try {
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(document);
                StreamResult result = new StreamResult(os);
                transformer.transform(source, result);
            } catch (TransformerException e) {
                throw new IOException(e);
            }
        }
    }

    @Override
    public List<? extends TargetDefinition.Location> getLocations() {
        return locations;
    }

    @Override
    public boolean hasIncludedBundles() {
        return hasIncludeBundles;
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
			return FILE_CACHE.computeIfAbsent(uri.normalize(), key -> {
                try {
					try (InputStream input = openTargetStream(uri)) {
						return parse(parseDocument(input), getOrigin(uri));
                    } catch (ParserConfigurationException e) {
                        throw new TargetDefinitionSyntaxException("No valid XML parser: " + e.getMessage(), e);
                    } catch (SAXException e) {
                        throw new TargetDefinitionSyntaxException(
                                "Target definition is not well-formed XML: " + e.getMessage(), e);
                    }
                } catch (IOException e) {
                    throw new TargetDefinitionSyntaxException(
                            "I/O error while reading target definition file: " + e.getMessage(), e);
                }

            });
        } catch (TargetDefinitionSyntaxException e) {
            throw new RuntimeException("Invalid syntax in target definition " + uri + ": " + e.getMessage(), e);
        }
    }

	private static String getOrigin(URI uri) {
		if (isDataUrl(uri)) {
			return "<embedded>";
		}
		return uri.toASCIIString();
	}

	private static InputStream openTargetStream(URI uri) throws IOException, MalformedURLException {
		if (isDataUrl(uri)) {
			String rawPath = uri.toASCIIString();
			int indexOf = rawPath.indexOf(',');
			if (indexOf > -1) {
				String data = rawPath.substring(indexOf + 1);
				return new ByteArrayInputStream(Base64.getDecoder().decode(data));
			} else {
				throw new MalformedURLException("invalid data url!");
			}
		}
		return uri.toURL().openStream();
	}

	private static boolean isDataUrl(URI uri) {
		return "data".equals(uri.getScheme());
	}

    public static TargetDefinitionFile parse(Document document, String origin) {
        return new TargetDefinitionFile(document, origin);
    }

    @Override
    public String getTargetEE() {
        return targetEE;
    }

    @Override
    public String toString() {
        return "TargetDefinitionFile[" + origin + "]";
    }

    /**
	 * List all target files in the given folder
	 * 
	 * @param folder
	 * @return the found target files or empty array if nothing was found, folder is not a directory
	 *         or the directory could not be read
	 */
	public static File[] listTargetFiles(File folder) {
	    if (folder.isDirectory()) {
	        File[] targetFiles = folder.listFiles(TargetDefinitionFile::isTargetFile);
	        if (targetFiles != null) {
	            return targetFiles;
	        }
	    }
	    return new File[0];
	}

	/**
	 * 
	 * @param file
	 * @return <code>true</code> if the given files likely denotes are targetfile based on file
	 *         naming, <code>false</code> otherwise
	 */
	public static boolean isTargetFile(File file) {
		return file != null && file.isFile()
				&& file.getName().toLowerCase().endsWith(TargetDefinitionFile.FILE_EXTENSION)
	            && !file.getName().startsWith(".polyglot.");
	}
	
	private static List<? extends TargetDefinition.Location> parseLocations(Element dom) {
        ArrayList<TargetDefinition.Location> locations = new ArrayList<>();
		Element locationsDom = getChild(dom, ELEMENT_LOCATIONS);
        if (locationsDom != null) {
            for (Element locationDom : getChildren(locationsDom, "location")) {
                String type = locationDom.getAttribute("type");
                if (InstallableUnitLocation.TYPE.equals(type)) {
                    locations.add(parseIULocation(locationDom));
                } else if ("Directory".equals(type)) {
                    locations.add(new DirectoryTargetLocation(locationDom.getAttribute("path")));
                } else if ("Profile".equals(type)) {
                    locations.add(new ProfileTargetPlatformLocation(locationDom.getAttribute("path")));
                } else if ("Feature".equals(type)) {
                    locations.add(new FeatureTargetPlatformLocation(locationDom.getAttribute("path"),
                            locationDom.getAttribute("id"), locationDom.getAttribute("version")));
                } else if (MavenGAVLocation.TYPE.equals(type)) {
                    locations.add(parseMavenLocation(locationDom));
                } else if ("Target".equals(type)) {
                    locations.add(new TargetRef(locationDom.getAttribute("uri")));
				} else if (TargetDefinition.RepositoryLocation.TYPE.equals(type)) {
					locations.add(parseRepositoryLocation(locationDom));
                } else {
                    locations.add(new OtherLocation(type));
                }
            }
        }
        return Collections.unmodifiableList(locations);
    }

	private static TargetDefinition.RepositoryLocation parseRepositoryLocation(Element dom) {
		String uri = dom.getAttribute("uri");
		NodeList childNodes = dom.getChildNodes();
		List<Requirement> requirements = IntStream.range(0, childNodes.getLength()).mapToObj(childNodes::item)
				.filter(Element.class::isInstance).map(Element.class::cast)
				.filter(element -> element.getNodeName().equalsIgnoreCase("require"))
				.flatMap(element -> {
					String textContent = element.getTextContent();
					Parameters parameters = new Parameters(textContent);
					return CapReqBuilder.getRequirementsFrom(parameters).stream();
				}).toList();
		return new OSGIRepositoryLocation(uri, requirements);
	}

    private static MavenLocation parseMavenLocation(Element dom) {
        Set<String> globalExcludes = new LinkedHashSet<>();
        for (Element element : getChildren(dom, "exclude")) {
            globalExcludes.add(element.getTextContent());
        }
        Collection<String> scopes = new ArrayList<>();
        String scope = dom.getAttribute("includeDependencyScope");
        if (dom.hasAttribute("includeDependencyScopes")) {
            String scopesAttribute = dom.getAttribute("includeDependencyScopes");
            for (String s : scopesAttribute.split(",")) {
                scopes.add(s.strip());
            }
        } else {
            //backward compat ...
            String SCOPE_COMPILE = "compile";
            String SCOPE_TEST = "test";
            String SCOPE_RUNTIME = "runtime";
            String SCOPE_PROVIDED = "provided";
            String SCOPE_SYSTEM = "system";
            if (scope == null || scope.isBlank() || SCOPE_COMPILE.equalsIgnoreCase(scope)) {
                scopes.add(SCOPE_COMPILE);
            } else if (SCOPE_PROVIDED.equalsIgnoreCase(scope)) {
                scopes.add(SCOPE_PROVIDED);
                scopes.add(SCOPE_COMPILE);
                scopes.add(SCOPE_SYSTEM);
                scopes.add(SCOPE_RUNTIME);
            } else if (SCOPE_TEST.equalsIgnoreCase(scope)) {
                scopes.add(SCOPE_TEST);
                scopes.add(SCOPE_COMPILE);
                scopes.add(SCOPE_PROVIDED);
                scopes.add(SCOPE_SYSTEM);
                scopes.add(SCOPE_RUNTIME);
            }
        }
        Element featureTemplate = getChild(dom, "feature");
        return new MavenLocation(parseRoots(dom, globalExcludes), scopes, parseManifestStrategy(dom),
                Boolean.parseBoolean(dom.getAttribute("includeSource")), parseInstructions(dom),
				parseDependencyDepth(dom, scope), parseRepositoryReferences(dom), featureTemplate,
				dom.getAttribute("label"));
    }

    private static IULocation parseIULocation(Element dom) {
        List<Unit> units = new ArrayList<>();
        for (Element unitDom : getChildren(dom, "unit")) {
            String id = unitDom.getAttribute("id");
            String version = unitDom.getAttribute("version");
            units.add(new Unit(id, version));
        }
        final List<Repository> repositories = new ArrayList<>();
        for (Element node : getChildren(dom, "repository")) {
            String id = node.getAttribute("id");
            String uri = node.getAttribute("location");
            repositories.add(new Repository(id, uri));
        }
        
        String rawFollowRepositoryReferences = dom.getAttribute("followRepositoryReferences");
        final FollowRepositoryReferences followRepositoryReferences;
        if (rawFollowRepositoryReferences == null || rawFollowRepositoryReferences.isEmpty()) {
            followRepositoryReferences = FollowRepositoryReferences.DEFAULT;
        } else if (Boolean.parseBoolean(rawFollowRepositoryReferences)) {
            followRepositoryReferences = FollowRepositoryReferences.ENABLED;
        } else {
            followRepositoryReferences = FollowRepositoryReferences.DISABLED;
        }
        
        return new IULocation(Collections.unmodifiableList(units), Collections.unmodifiableList(repositories),
                parseIncludeMode(dom), Boolean.parseBoolean(dom.getAttribute("includeAllPlatforms")),
                Boolean.parseBoolean(dom.getAttribute("includeSource")),
                Boolean.parseBoolean(dom.getAttribute("includeConfigurePhase")),
                followRepositoryReferences
                );
    }

    private static String parseTargetEE(Element dom) {
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

    private static IncludeMode parseIncludeMode(Element dom) {
        Attr attributeValue = dom.getAttributeNode("includeMode");
        if (attributeValue == null || "planner".equals(attributeValue.getTextContent())) {
            return IncludeMode.PLANNER;
        } else if ("slicer".equals(attributeValue.getTextContent())) {
            return IncludeMode.SLICER;
        }
        throw new TargetDefinitionSyntaxException("Invalid value for attribute 'includeMode': " + attributeValue + "");
    }

    private static MissingManifestStrategy parseManifestStrategy(Element dom) {
        String attributeValue = dom.getAttribute("missingManifest");
        if ("generate".equalsIgnoreCase(attributeValue)) {
            return MissingManifestStrategy.GENERATE;
        } else if ("ignore".equals(attributeValue)) {
            return MissingManifestStrategy.IGNORE;
        }
        return MissingManifestStrategy.ERROR;
    }

    private static Collection<BNDInstructions> parseInstructions(Element dom) {
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
        return Collections.unmodifiableCollection(list);
    }

    private static Collection<MavenDependency> parseRoots(Element dom, Set<String> globalExcludes) {
        for (Element dependencies : getChildren(dom, "dependencies")) {
            List<MavenDependency> roots = new ArrayList<>();
            for (Element dependency : getChildren(dependencies, "dependency")) {
                roots.add(parseDependecyRoot(dependency, globalExcludes));
            }
            return Collections.unmodifiableCollection(roots);
        }
        //backward compatibility for old format...
        return Collections.singleton(parseDependecyRoot(dom, globalExcludes));
    }

    private static MavenDependencyRoot parseDependecyRoot(Element dom, Set<String> globalExcludes) {
        return new MavenDependencyRoot(//
                getTextFromChild(dom, "groupId", null), //
                getTextFromChild(dom, "artifactId", null), //
                getTextFromChild(dom, "version", null), //
                getTextFromChild(dom, "classifier", ""), //
                getTextFromChild(dom, "type", "jar"), //
                globalExcludes);
    }

    private static DependencyDepth parseDependencyDepth(Element dom, String scope) {
        if (dom.getAttributeNode("includeDependencyDepth") == null) {
            //backward compat
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

    private static Collection<MavenArtifactRepositoryReference> parseRepositoryReferences(Element dom) {
        for (Element dependencies : getChildren(dom, "repositories")) {
            List<MavenArtifactRepositoryReference> list = new ArrayList<>();
            for (Element repository : getChildren(dependencies, "repository")) {
                String id = getTextFromChild(repository, "id", String.valueOf(System.identityHashCode(repository)));
                String url = getTextFromChild(repository, "url", null);
                list.add(new MavenArtifactRepositoryReference() {

                    @Override
                    public String getId() {
                        return id;
                    }

                    @Override
                    public String getUrl() {
                        return url;
                    }

                });
            }
            return Collections.unmodifiableCollection(list);
        }
        return Collections.emptyList();
    }

}
