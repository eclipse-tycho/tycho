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
package org.eclipse.tycho.apitools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;
import de.pdark.decentxml.XMLWriter;

public class LogFileEnhancer {

	private static final String SEVERITY_ERROR = "ERROR";
	private static final String SEVERITY_WARNING = "WARNING";
	private static final String ATTRIBUTES_WARNINGS = "warnings";
	private static final String ELEMENT_PROBLEMS = "problems";
	private static final String ATTRIBUTES_PROBLEMS = "problems";
	private static final String ATTRIBUTES_INFOS = "infos";
	private static final String ATTRIBUTES_ERRORS = "errors";

	public static void enhanceXml(File logDirectory, ApiAnalysisResult analysisResult) throws IOException {
		Map<String, List<IApiProblem>> problems = analysisResult.problems()
				.collect(Collectors.groupingBy(IApiProblem::getResourcePath));
		if (problems.isEmpty()) {
			return;
		}
		Set<File> needsUpdate = new HashSet<>();
		Map<File, Document> documents = readDocuments(logDirectory);
		for (Entry<String, List<IApiProblem>> problemEntry : problems.entrySet()) {
			String path = problemEntry.getKey();
			for (Entry<File, Document> documentEntry : documents.entrySet()) {
				Document document = documentEntry.getValue();
				Element statsElement = getStatsElement(document);
				for (Element sources : document.getRootElement().getChildren("sources")) {
					for (Element source : sources.getChildren("source")) {
						String pathAttribute = source.getAttributeValue("path");
						if (pathAttribute != null && !pathAttribute.isEmpty() && pathAttribute.endsWith(path)) {
							needsUpdate.add(documentEntry.getKey());
							Element problemsElement = getProblemsElement(source);
							List<IApiProblem> list = problemEntry.getValue();
							Map<Integer, List<IApiProblem>> problemsBySeverity = list.stream()
									.collect(Collectors.groupingBy(IApiProblem::getSeverity));
							List<IApiProblem> errors = problemsBySeverity.getOrDefault(ApiPlugin.SEVERITY_ERROR,
									List.of());
							List<IApiProblem> warnings = problemsBySeverity.getOrDefault(ApiPlugin.SEVERITY_WARNING,
									List.of());
							incrementAttribute(problemsElement, ATTRIBUTES_PROBLEMS, list.size());
							incrementAttribute(problemsElement, ATTRIBUTES_WARNINGS, warnings.size());
							incrementAttribute(problemsElement, ATTRIBUTES_ERRORS, errors.size());
							if (statsElement != null) {
								incrementAttribute(statsElement, ATTRIBUTES_PROBLEMS, list.size());
								incrementAttribute(statsElement, ATTRIBUTES_WARNINGS, warnings.size());
								incrementAttribute(statsElement, ATTRIBUTES_ERRORS, errors.size());
							}
							for (IApiProblem problem : warnings) {
								addProblem(problemsElement, problem, SEVERITY_WARNING);
							}
							for (IApiProblem problem : errors) {
								addProblem(problemsElement, problem, SEVERITY_ERROR);
							}
						}
					}
				}
			}

		}
		writeDocuments(needsUpdate, documents);
	}

	private static Element getStatsElement(Document document) {
		for (Element stats : document.getRootElement().getChildren("stats")) {
			for (Element problem_summary : stats.getChildren("problem_summary")) {
				return problem_summary;
			}
		}
		return null;
	}

	private static void addProblem(Element problemsElement, IApiProblem problem, String severity) {
		Element element = new Element("problem");
		element.setAttribute("line", Integer.toString(problem.getLineNumber()));
		element.setAttribute("severity", severity);
		element.setAttribute("charStart", Integer.toString(problem.getCharStart()));
		element.setAttribute("charEnd", Integer.toString(problem.getCharEnd()));
		element.setAttribute("categoryID", Integer.toString(problem.getCategory()));
		element.setAttribute("problemID", Integer.toString(problem.getId()));
		Element messageElement = new Element("message");
		messageElement.setAttribute("value", problem.getMessage());
		element.addNode(messageElement);
		problemsElement.addNode(element);
	}

	private static void incrementAttribute(Element element, String attribute, int increment) {
		if (increment > 0) {
			int current = Integer.parseInt(element.getAttributeValue(attribute));
			element.setAttribute(attribute, Integer.toString(current + increment));
		}
	}

	private static void writeDocuments(Set<File> needsUpdate, Map<File, Document> documents)
			throws IOException, FileNotFoundException {
		for (File file : needsUpdate) {
			Document document = documents.get(file);
			try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
					XMLWriter xw = new XMLWriter(w)) {
				document.toXML(xw);
			}
		}
	}

	private static Map<File, Document> readDocuments(File logDirectory) throws IOException {
		XMLParser parser = new XMLParser();
		Map<File, Document> documents = new HashMap<>();
		for (File child : logDirectory.listFiles()) {
			if (child.getName().toLowerCase().endsWith(".xml")) {
				documents.put(child, parser.parse(new XMLIOSource(child)));
			}
		}
		return documents;
	}

	private static Element getProblemsElement(Element source) {
		Element element = source.getChild(ELEMENT_PROBLEMS);
		if (element == null) {
			element = new Element(ELEMENT_PROBLEMS);
			element.setAttribute(ATTRIBUTES_ERRORS, "0");
			element.setAttribute(ATTRIBUTES_INFOS, "0");
			element.setAttribute(ATTRIBUTES_PROBLEMS, "0");
			element.setAttribute(ATTRIBUTES_WARNINGS, "0");
			source.addNode(0, element);
		}
		return element;
	}

}
