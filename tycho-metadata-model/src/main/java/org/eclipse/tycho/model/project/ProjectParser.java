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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class ProjectParser {

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
            };
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException("parsing failed", e);
        }
    }

}
