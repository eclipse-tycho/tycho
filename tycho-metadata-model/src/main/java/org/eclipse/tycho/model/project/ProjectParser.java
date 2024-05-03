/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
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
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
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
            NodeList linkNodes = root.getElementsByTagName("link");
            Map<Path, LinkDescription> links = IntStream.range(0, linkNodes.getLength())
                    .mapToObj(i -> linkNodes.item(i)).map(Element.class::cast).map(e -> parseLink(e))
                    .filter(Objects::nonNull).collect(Collectors.toMap(LinkDescription::name, Function.identity()));

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
                    for (Entry<Path, LinkDescription> entry : links.entrySet()) {
                        Path linkPath = entry.getKey();
                        if (relative.startsWith(linkPath)) {
                            LinkDescription link = entry.getValue();
                            if (link.type() == LinkDescription.FILE) {
                                //the path must actually match each others as it is a file!
                                if (linkPath.startsWith(relative)) {
                                    Path resolvedPath = resolvePath(link.locationURI(), this);
                                    return location.resolve(resolvedPath).normalize();
                                }
                            } else if (link.type() == LinkDescription.FOLDER) {
                                Path linkRelative = linkPath.relativize(relative);
                                Path resolvedPath = resolvePath(link.locationURI(), this);
                                if (resolvedPath != null) {
                                    return location.resolve(resolvedPath).resolve(linkRelative).normalize();
                                }
                            }
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

    private static Path resolvePath(URI uri, EclipseProject project) {
        String schemeSpecificPart = uri.getSchemeSpecificPart();
        if (schemeSpecificPart != null) {
            Path path = Path.of(schemeSpecificPart);
            int count = path.getNameCount();
            if (count > 0) {
                //only the first path is allowed to be a variable...
                Path first = path.getName(0);
                String name = first.toString();
                if ("PROJECT_LOC".equals(name)) {
                    return appendRemaining(path, count, project.getLocation());
                }
                Matcher parentMatcher = PARENT_PROJECT_PATTERN.matcher(name);
                if (parentMatcher.matches()) {
                    Path resolvedPath = Path.of("..");
                    int p = Integer.parseInt(parentMatcher.group(1));
                    for (int i = 1; i < p; i++) {
                        resolvedPath = resolvedPath.resolve("..");
                    }
                    return appendRemaining(path, count, resolvedPath);
                }
                return path;
            }
        }
        return null;
    }

    private static Path appendRemaining(Path path, int count, Path resolvedPath) {
        for (int i = 1; i < count; i++) {
            resolvedPath = resolvedPath.resolve(path.getName(i));
        }
        return resolvedPath;
    }

    private static ProjectVariable parseVariable(Element element) {
        try {
            String name = element.getElementsByTagName("name").item(0).getTextContent();
            String value = element.getElementsByTagName("value").item(0).getTextContent();
            return new ProjectVariable(name, value);
        } catch (RuntimeException e) {
            //something is wrong here...
            return null;
        }
    }

    private static LinkDescription parseLink(Element element) {
        try {
            String name = element.getElementsByTagName("name").item(0).getTextContent();
            String type = element.getElementsByTagName("type").item(0).getTextContent();
            String locationURI = element.getElementsByTagName("locationURI").item(0).getTextContent();
            return new LinkDescription(Path.of(name), Integer.parseInt(type), URI.create(locationURI));
        } catch (RuntimeException e) {
            //something is wrong here...
            return null;
        }
    }

    static final record LinkDescription(Path name, int type, URI locationURI) {
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
