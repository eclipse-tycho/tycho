/*******************************************************************************
 * Copyright (c) 2023, 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *    SAP SE - support custom variables
 *******************************************************************************/
package org.eclipse.tycho.model.project;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class ProjectParser {

    private static final String PROJECT_LOC = "PROJECT_LOC";
    private static final Pattern DOLLAR_VARIABLE_PATTERN = Pattern.compile("\\$\\{([^\\$]+)\\}");
    private static final Pattern PARENT_PROJECT_PATTERN = Pattern.compile("PARENT-(\\d+)-PROJECT_LOC");

    public static EclipseProject parse(Path path) throws IOException {
        try (InputStream stream = Files.newInputStream(path)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(stream);
            Element root = doc.getDocumentElement();
            Node nameNode = root.getElementsByTagName("name").item(0);
            Node commentNode = root.getElementsByTagName("comment").item(0);
            String name;
            Path location = path.getParent();
            if (nameNode != null) {
                name = nameNode.getTextContent().strip();
            } else {
                name = location.getFileName().toString();
            }
            String comment;
            if (commentNode == null) {
                comment = null;
            } else {
                comment = commentNode.getTextContent();
            }
            NodeList natureNodes = root.getElementsByTagName("nature");
            int length = natureNodes.getLength();
            Set<String> natureSet = new HashSet<>();
            for (int i = 0; i < length; i++) {
                natureSet.add(natureNodes.item(i).getTextContent());
            }
            NodeList variableNodes = root.getElementsByTagName("variable");
            List<ProjectVariable> variables = IntStream.range(0, variableNodes.getLength())
                    .mapToObj(i -> variableNodes.item(i)).map(Element.class::cast).map(e -> parseVariable(e))
                    .filter(Objects::nonNull).toList();
            Map<String, URI> variablesMap = variables.stream()
                    .collect(Collectors.toMap(ProjectVariable::name, ProjectVariable::value));
            NodeList linkNodes = root.getElementsByTagName("link");
            Set<LinkDescription> links = IntStream.range(0, linkNodes.getLength()).mapToObj(i -> linkNodes.item(i))
                    .map(Element.class::cast).map(e -> parseLink(e)).filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            return new EclipseProject() {

                @Override
                public String toString() {
                    return getName() + " @ " + getLocation();
                }

                @Override
                public int hashCode() {
                    return getLocation().hashCode();
                }

                @Override
                public boolean equals(Object obj) {
                    if (obj instanceof EclipseProject p) {
                        return p.getLocation().equals(getLocation());
                    }
                    return false;
                }

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public Path getLocation() {
                    return location;
                }

                @Override
                public boolean hasNature(String nature) {
                    return natureSet.contains(nature);
                }

                @Override
                public String getComment() {
                    if (comment == null || comment.isBlank()) {
                        return null;
                    }
                    return comment;
                }

                @Override
                public Path getFile(Path path) {
                    if (path == null) {
                        return null;
                    }
                    if (!path.isAbsolute()) {
                        path = location.resolve(path);
                    }
                    if (Files.isRegularFile(path)) {
                        //if the file is already there we don't need to bother any links
                        return path;
                    }
                    Path relative = location.relativize(path);
                    for (LinkDescription link : links) {
                        if (relative.startsWith(link.name())) {
                            return resolvePath(link, relative, location, variablesMap);
                        }
                    }
                    return path;
                }

                @Override
                public Path getFile(String path) {
                    return getFile(location.resolve(path));
                }

                @Override
                public Collection<ProjectVariable> getVariables() {
                    return variables;
                }
            };
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException("parsing failed", e);
        }
    }

    private static Path uriToPath(URI uri) {
        if (uri.getScheme() == null || "file".equals(uri.getScheme())) {
            return Path.of(uri.getSchemeSpecificPart());
        }
        return null;
    }

    static Path resolvePath(LinkDescription link, Path path, Path projectLocation, Map<String, URI> variablesMap) {
        Path linkPath;
        if (link.location() != null) {
            linkPath = link.location();
        } else if (link.locationURI() != null) {
            URI uri = link.locationURI();
            if (uri == null) {
                return null;
            }
            String schemeSpecificPart = uri.getSchemeSpecificPart();
            if (schemeSpecificPart == null) {
                return null;
            }
            linkPath = Path.of(schemeSpecificPart);
            int count = linkPath.getNameCount();
            if (count == 0) {
                return null;
            }
        } else {
            return null;
        }
        if (link.type() == LinkDescription.FILE) {
            //the path must actually match each others as it is a file!
            if (path.getNameCount() > 1) {
                return null;
            }
        }

        Path result = resolveVariables(linkPath, projectLocation, variablesMap);
        if (result == null) {
            return null;
        }
        result = projectLocation.resolve(result);
        result = appendRemaining(path, result);
        result = result.normalize();

        return result;
    }

    private static Path resolveVariables(Path path, Path projectLocation, Map<String, URI> variablesMap) {
        //only the first path is allowed to be a variable...
        Path first = path.getName(0);
        String name = first.toString();

        // Expand user-defined variables
        // https://github.com/eclipse-tycho/tycho/issues/3820
        // Note: Eclipse supports many more user-defined variables and even nesting
        // This is a best-effort implementation to support the straight-forward case:
        // If the first segment matches one of the user-defined variables, e.g.
        // {
        //   "FOO": "${BAR}/foo",
        //   "BAR": "${BAZ}/bar",
        //   "BAZ": "${PROJECT-2-PARENT_LOC}/baz"
        // }
        // we expand it recursively:
        // FOO/dir -> ${BAR}/foo/dir -> ${BAZ}/bar/foo/dir -> ${PROJECT-2-PARENT_LOC}/baz/bar/foo/dir
        // When we find known placeholders, we continue with that logic:
        // ${PROJECT-2-PARENT_LOC}/baz/bar/foo/dir -> ../../baz/bar/foo/dir
        URI variableUri = variablesMap.get(name);
        if (variableUri != null) {
            Path variablePath = uriToPath(variableUri);
            if (variablePath == null) {
                return null;
            }

            Path[] resolvedPath = new Path[] { variablePath };
            Set<String> seenVariables = new HashSet<>(Set.of(name));
            while (resolveDollarVariable(resolvedPath, projectLocation, seenVariables, variablesMap)) {
            }
            if (resolvedPath[0] == null) {
                return null;
            }
            return appendRemaining(path, resolvedPath[0]);
        }

        if (PROJECT_LOC.equals(name)) {
            return appendRemaining(path, projectLocation);
        }
        Matcher parentMatcher = PARENT_PROJECT_PATTERN.matcher(name);
        if (parentMatcher.matches()) {
            return appendRemaining(path, dotDotTimes(Integer.parseInt(parentMatcher.group(1))));
        }

        return path;
    }

    private static boolean resolveDollarVariable(Path[] resolvedPath, Path projectLocation, Set<String> seenVariables,
            Map<String, URI> variablesMap) {

        Matcher dollarVariableMatcher = DOLLAR_VARIABLE_PATTERN.matcher(resolvedPath[0].getName(0).toString());
        if (!dollarVariableMatcher.matches()) {
            return false;
        }
        String expanded = dollarVariableMatcher.group(1);
        if (PROJECT_LOC.equals(expanded)) {
            resolvedPath[0] = appendRemaining(resolvedPath[0], projectLocation);
            return false;
        }
        Matcher parentMatcher = PARENT_PROJECT_PATTERN.matcher(expanded);
        if (parentMatcher.matches()) {
            resolvedPath[0] = appendRemaining(resolvedPath[0], dotDotTimes(Integer.parseInt(parentMatcher.group(1))));
            return false;
        }
        URI variableUri = variablesMap.get(expanded);
        if (variableUri == null) {
            // give up and leave path unexpanded
            return false;
        }
        Path variablePath = uriToPath(variableUri);
        if (variablePath == null) {
            resolvedPath[0] = null;
            return false;
        }
        if (!seenVariables.add(expanded)) {
            resolvedPath[0] = null; // recursion -> invalid
            return false;
        }
        resolvedPath[0] = appendRemaining(resolvedPath[0], variablePath);
        return true;
    }

    private static Path dotDotTimes(int count) {
        Path path = Path.of("..");
        for (int i = 1; i < count; i++) {
            path = path.resolve("..");
        }
        return path;
    }

    private static Path appendRemaining(Path path, Path resolvedPath) {
        int count = path.getNameCount();
        for (int i = 1; i < count; i++) {
            resolvedPath = resolvedPath.resolve(path.getName(i));
        }
        return resolvedPath;
    }

    private static ProjectVariable parseVariable(Element element) {
        try {
            String name = element.getElementsByTagName("name").item(0).getTextContent();
            String value = element.getElementsByTagName("value").item(0).getTextContent();
            return new ProjectVariable(name, URI.create(value));
        } catch (RuntimeException e) {
            //something is wrong here...
            return null;
        }
    }

    private static LinkDescription parseLink(Element element) {
        try {
            String name = element.getElementsByTagName("name").item(0).getTextContent();
            String type = element.getElementsByTagName("type").item(0).getTextContent();
            Node locationNode = element.getElementsByTagName("location").item(0);
            Node locationUriNode = element.getElementsByTagName("locationURI").item(0);
            Path location = locationNode == null ? null : Path.of(locationNode.getTextContent());
            URI locationURI = locationUriNode == null ? null : URI.create(locationUriNode.getTextContent());
            return new LinkDescription(Path.of(name), Integer.parseInt(type), location, locationURI);
        } catch (RuntimeException e) {
            //something is wrong here...
            return null;
        }
    }

    static final record LinkDescription(Path name, int type, Path location, URI locationURI) {
        /**
         * Type constant (bit mask value 1) which identifies file resources.
         */
        static final int FILE = 0x1;

        /**
         * Type constant (bit mask value 2) which identifies folder resources.
         */
        static final int FOLDER = 0x2;

        /**
         * Type constant (bit mask value 4) which identifies project resources.
         */
        static final int PROJECT = 0x4;

        /**
         * Type constant (bit mask value 8) which identifies the root resource.
         */
        static final int ROOT = 0x8;

    }

}
