/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christoph Läubrich  - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Component(role = ClasspathParser.class)
public class JDTClasspathParser implements ClasspathParser, Disposable {

    private Map<String, JDTClasspath> cache = new ConcurrentHashMap<>();

    @Override
    public void dispose() {
        cache.clear();
    }

    @Override
    public Collection<ProjectClasspathEntry> parse(File basedir) throws IOException {
        File file = new File(basedir, ".classpath");
        if (file.exists()) {
            return cache.computeIfAbsent(file.getCanonicalPath(), f -> new JDTClasspath(file)).getEntries();
        }
        return Collections.emptyList();
    }

    private static final class JDTClasspath {

        private File file;

        private List<ProjectClasspathEntry> entries;

        public JDTClasspath(File file) {
            this.file = file;
        }

        public synchronized Collection<ProjectClasspathEntry> getEntries() throws IOException {
            if (entries == null) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder;
                try {
                    builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(file);
                    NodeList classpathentries = doc.getDocumentElement().getElementsByTagName("classpathentry");
                    int length = classpathentries.getLength();
                    List<ProjectClasspathEntry> list = new ArrayList<>();
                    String defaultOutput = "bin";
                    for (int i = 0; i < length; i++) {
                        Element classpathentry = (Element) classpathentries.item(i);
                        String kind = classpathentry.getAttribute("kind");
                        if ("output".equals(kind)) {
                            defaultOutput = classpathentry.getAttribute("path");
                        }
                    }
                    for (int i = 0; i < length; i++) {
                        Element classpathentry = (Element) classpathentries.item(i);
                        Map<String, String> attributes = getAttributes(classpathentry);

                        String kind = classpathentry.getAttribute("kind");
                        if ("src".equals(kind)) {
                            String path = classpathentry.getAttribute("path");
                            String output = classpathentry.getAttribute("output");
                            if (output.isBlank()) {
                                output = defaultOutput;
                            }
                            list.add(new JDTSourceFolder(new File(file.getParentFile(), path),
                                    new File(file.getParentFile(), output), attributes));
                        }
                    }
                    entries = Collections.unmodifiableList(list);
                } catch (ParserConfigurationException e) {
                    throw new IOException(e);
                } catch (SAXException e) {
                    throw new IOException(e);
                }
            }
            return entries;
        }

        private Map<String, String> getAttributes(Element parent) {
            Map<String, String> map = new HashMap<>();
            NodeList attributes = parent.getElementsByTagName("attribute");
            int length = attributes.getLength();
            for (int i = 0; i < length; i++) {
                Element attribute = (Element) attributes.item(i);
                map.put(attribute.getAttribute("name"), attribute.getAttribute("value"));
            }
            return map;
        }

    }

    private static class JDTSourceFolder implements SourceFolderClasspathEntry {

        private File path;
        private Map<String, String> attributes;
        private File output;

        public JDTSourceFolder(File path, File output, Map<String, String> attributes) {
            this.path = path;
            this.output = output;
            this.attributes = attributes;
        }

        @Override
        public File getSourcePath() {
            return path;
        }

        @Override
        public File getOutputFolder() {
            return output;
        }

        @Override
        public Map<String, String> getAttributes() {
            return attributes;
        }

    }

}
