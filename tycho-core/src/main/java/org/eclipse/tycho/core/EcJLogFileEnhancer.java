/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
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
package org.eclipse.tycho.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;

import eu.maveniverse.domtrip.Serializer;

/**
 * This allows to enhance the ECJ logfile with additional warnings/problem if needed
 */
public class EcJLogFileEnhancer implements AutoCloseable {

    public static final String SEVERITY_ERROR = "ERROR";
    public static final String SEVERITY_WARNING = "WARNING";
    private static final String ATTRIBUTES_WARNINGS = "warnings";
    private static final String ELEMENT_PROBLEMS = "problems";
    private static final String ATTRIBUTES_PROBLEMS = "problems";
    private static final String ATTRIBUTES_INFOS = "infos";
    private static final String ATTRIBUTES_ERRORS = "errors";

    private Set<File> needsUpdate = new HashSet<>();
    private Map<File, Document> documents;

    private EcJLogFileEnhancer(Map<File, Document> documents) {
        this.documents = documents;
    }

    public Stream<Source> sources() {
        return documents.entrySet().stream().flatMap(documentEntry -> {
            Document document = documentEntry.getValue();
            Element statsElement = getStatsElement(document);
            File file = documentEntry.getKey();
            return document.root().children("sources").stream()
                    .flatMap(sources -> sources.children("source").stream())
                    .map(source -> new Source(source, statsElement, () -> needsUpdate.add(file)));
        });
    }

    @Override
    public void close() throws IOException {
        writeDocuments(needsUpdate, documents);
    }

    public static EcJLogFileEnhancer create(File logDirectory) throws IOException {
        Map<File, Document> documents = readDocuments(logDirectory);
        return new EcJLogFileEnhancer(documents);
    }

    private static Element getStatsElement(Document document) {
        for (Element stats : document.root().children("stats")) {
            for (Element problem_summary : stats.children("problem_summary").toList()) {
                return problem_summary;
            }
        }
        return null;
    }

    private static void incrementAttribute(Element element, String attribute, int increment) {
        if (increment > 0) {
            int current = Integer.parseInt(element.attribute(attribute));
            element.attribute(attribute, Integer.toString(current + increment));
        }
    }

    private static void writeDocuments(Set<File> needsUpdate, Map<File, Document> documents)
            throws IOException, FileNotFoundException {
        for (File file : needsUpdate) {
            Document document = documents.get(file);
            try (Writer w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
                    XMLWriter xw = new XMLWriter(w)) {
                document.toXML(xw);
            }
        }
    }

    private static Map<File, Document> readDocuments(File logDirectory) throws IOException {
        XMLParser parser = new XMLParser();
        Map<File, Document> documents = new HashMap<>();
        for (File child : logDirectory.listFiles()) {
            if (child.name().toLowerCase().endsWith(".xml")) {
                documents.put(child, Document.of(child));
            }
        }
        return documents;
    }

    private static Element getProblemsElement(Element source) {
        Element element = source.getChild(ELEMENT_PROBLEMS);
        if (element == null) {
            element = Element.of(ELEMENT_PROBLEMS);
            element.attribute(ATTRIBUTES_ERRORS, "0");
            element.attribute(ATTRIBUTES_INFOS, "0");
            element.attribute(ATTRIBUTES_PROBLEMS, "0");
            element.attribute(ATTRIBUTES_WARNINGS, "0");
            source.addNode(0, element);
        }
        return element;
    }

    public static class Source {

        private Element source;
        private Element statsElement;
        private Runnable needsUpdate;

        Source(Element source, Element statsElement, Runnable needsUpdate) {
            this.source = source;
            this.statsElement = statsElement;
            this.needsUpdate = needsUpdate;
        }

        public String getPath() {
            return source.attribute("path");
        }

        public String getOutputDirectory() {
            return source.attribute("output");
        }

        public String getPackage() {
            return source.attribute("package");
        }

        public void addProblem(String severity, int lineNumber, int charStart, int charEnd, int categoryId,
                int problemId, String message) {
            Element problemsElement = getProblemsElement(source);
            Element element = Element.of("problem");
            element.attribute("line", Integer.toString(lineNumber));
            element.attribute("severity", severity);
            element.attribute("id", Integer.toString(problemId));
            element.attribute("charStart", Integer.toString(charStart));
            element.attribute("charEnd", Integer.toString(charEnd));
            element.attribute("categoryID", Integer.toString(categoryId));
            element.attribute("problemID", Integer.toString(problemId));
            Element messageElement = Element.of("message");
            messageElement.attribute("value", message);
            element.addNode(messageElement);
            incrementAttribute(problemsElement, ATTRIBUTES_PROBLEMS, 1);
            if (SEVERITY_ERROR.equals(severity)) {
                incrementAttribute(problemsElement, ATTRIBUTES_ERRORS, 1);
            }
            if (SEVERITY_WARNING.equals(severity)) {
                incrementAttribute(problemsElement, ATTRIBUTES_WARNINGS, 1);
            }
            if (statsElement != null) {
                incrementAttribute(statsElement, ATTRIBUTES_PROBLEMS, 1);
                if (SEVERITY_ERROR.equals(severity)) {
                    incrementAttribute(statsElement, ATTRIBUTES_ERRORS, 1);
                }
                if (SEVERITY_WARNING.equals(severity)) {
                    incrementAttribute(statsElement, ATTRIBUTES_WARNINGS, 1);
                }
            }
            problemsElement.addNode(element);
            needsUpdate.run();
        }

        public boolean hasClass(String classFile) {
            return source.children("classfile").stream().map(elem -> elem.attribute("path"))
                    .filter(Objects::nonNull).anyMatch(path -> path.endsWith(classFile));
        }

    }

}
