/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christoph Läubrich  - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.dotClasspath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.eclipse.tycho.ArtifactKey;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Component(role = ClasspathParser.class)
public class ClasspathParser implements Disposable {

    private Map<String, JDTClasspath> cache = new ConcurrentHashMap<>();

    @Override
    public void dispose() {
        cache.clear();
    }

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
                        } else if ("con".equals(kind)) {
                            String path = classpathentry.getAttribute("path");
                            if (path.startsWith(JUnitClasspathContainerEntry.JUNIT_CONTAINER_PATH_PREFIX)) {
                                String junit = path
                                        .substring(JUnitClasspathContainerEntry.JUNIT_CONTAINER_PATH_PREFIX.length());
                                list.add(new JDTJUnitContainerClasspathEntry(path, junit, attributes));
                            } else if (path.equals(JREClasspathEntry.JRE_CONTAINER_PATH)
                                    || path.startsWith(JREClasspathEntry.JRE_CONTAINER_PATH_STANDARDVMTYPE_PREFIX)) {
                                list.add(new JDTJREClasspathEntry(path, attributes));
                            } else {
                                list.add(new JDTContainerClasspathEntry(path, attributes));
                            }
                        } else if ("lib".equals(kind)) {
                            String path = classpathentry.getAttribute("path");
                            list.add(new JDTLibraryClasspathEntry(new File(file.getParentFile(), path), attributes));
                        } else if ("var".equals(kind)) {
                            String path = classpathentry.getAttribute("path");
                            if (path.startsWith(M2ClasspathVariable.M2_REPO_VARIABLE_PREFIX)) {
                                String repoPath = path.substring(M2ClasspathVariable.M2_REPO_VARIABLE_PREFIX.length());
                                list.add(new M2E(repoPath, attributes));
                            }
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

    private static final class JDTJREClasspathEntry extends JDTContainerClasspathEntry implements JREClasspathEntry {

        public JDTJREClasspathEntry(String path, Map<String, String> attributes) {
            super(path, attributes);
        }

        @Override
        public boolean isModule() {
            return Boolean.valueOf(attributes.get("module"));
        }

        @Override
        public Collection<String> getLimitModules() {
            String modules = attributes.get("limit-modules");
            if (modules != null) {
                return Arrays.asList(modules.split(","));
            }
            return Collections.emptyList();
        }

        @Override
        public String getJREName() {
            if (path.startsWith(JRE_CONTAINER_PATH_STANDARDVMTYPE_PREFIX)) {
                return path.substring(JRE_CONTAINER_PATH_STANDARDVMTYPE_PREFIX.length());
            }
            return null;
        }

    }

    private static class JDTJUnitContainerClasspathEntry extends JDTContainerClasspathEntry
            implements JUnitClasspathContainerEntry {

        private final String junit;

        public JDTJUnitContainerClasspathEntry(String path, String junit, Map<String, String> attributes) {
            super(path, attributes);
            this.junit = junit;
        }

        @Override
        public String getJUnitSegment() {
            return junit;
        }

        @Override
        public Collection<ArtifactKey> getArtifacts() {
            if (JUNIT3.equals(junit)) {
                return Arrays.asList(JUNIT3_PLUGIN);
            } else if (JUNIT4.equals(junit)) {
                return Arrays.asList(JUNIT4_PLUGIN, HAMCREST_CORE_PLUGIN);
            } else if (JUNIT5.equals(junit)) {
                return Arrays.asList(JUNIT_JUPITER_API_PLUGIN, JUNIT_JUPITER_ENGINE_PLUGIN,
                        JUNIT_JUPITER_MIGRATIONSUPPORT_PLUGIN, JUNIT_JUPITER_PARAMS_PLUGIN,
                        JUNIT_PLATFORM_COMMONS_PLUGIN, JUNIT_PLATFORM_ENGINE_PLUGIN, JUNIT_PLATFORM_LAUNCHER_PLUGIN,
                        JUNIT_PLATFORM_RUNNER_PLUGIN, JUNIT_PLATFORM_SUITE_API_PLUGIN, JUNIT_VINTAGE_ENGINE_PLUGIN,
                        JUNIT_OPENTEST4J_PLUGIN, JUNIT_APIGUARDIAN_PLUGIN, JUNIT4_PLUGIN, HAMCREST_CORE_PLUGIN);
            }
            return Collections.emptyList();
        }

    }

    private static class JDTContainerClasspathEntry implements ClasspathContainerEntry {

        protected final String path;
        protected final Map<String, String> attributes;

        public JDTContainerClasspathEntry(String path, Map<String, String> attributes) {
            this.path = path;
            this.attributes = attributes;
        }

        @Override
        public Map<String, String> getAttributes() {
            return attributes;
        }

        @Override
        public String getContainerPath() {
            return path;
        }

    }

    private static final class JDTSourceFolder implements SourceFolderClasspathEntry {

        private final File path;
        private final Map<String, String> attributes;
        private final File output;

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

    private static final class M2E implements M2ClasspathVariable {

        private final String repoPath;
        private final Map<String, String> attributes;

        M2E(String repoPath, Map<String, String> attributes) {
            this.repoPath = repoPath;
            this.attributes = attributes;
        }

        @Override
        public Map<String, String> getAttributes() {
            return attributes;
        }

        @Override
        public String getRepositoryPath() {
            return repoPath;
        }

    }

    private static final class JDTLibraryClasspathEntry implements LibraryClasspathEntry {

        private final File path;
        private final Map<String, String> attributes;

        public JDTLibraryClasspathEntry(File path, Map<String, String> attributes) {
            this.path = path;
            this.attributes = attributes;
        }

        @Override
        public Map<String, String> getAttributes() {
            return attributes;
        }

        @Override
        public File getLibraryPath() {
            return path;
        }

    }

}
