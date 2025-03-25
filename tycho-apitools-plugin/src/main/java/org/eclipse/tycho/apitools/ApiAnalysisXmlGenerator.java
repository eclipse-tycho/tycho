/*******************************************************************************
 * Copyright (c) 2025 SAP SE and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.apitools;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.pde.api.tools.internal.problems.ApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

// Creates an XML report understood by warning-ng's native parser.
public class ApiAnalysisXmlGenerator {

	// Default, not actually checked by warnings-ng
	private static final String TAG_REPORT = "report";

	// See edu.hm.hafner.analysis.IssueParser
	private static final String TAG_ADDITIONAL_ATTRIBUTES = "additionalAttributes";
	private static final String TAG_CATEGORY = "category";
	private static final String TAG_COLUMN_END = "columnEnd";
	private static final String TAG_COLUMN_START = "columnStart";
	private static final String TAG_FILE_NAME = "fileName";
	private static final String TAG_ISSUE = "issue";
	private static final String TAG_LINE_END = "lineEnd";
	private static final String TAG_LINE_START = "lineStart";
	private static final String TAG_MESSAGE = "message";
	private static final String TAG_MODULE_NAME = "moduleName";
	private static final String TAG_PACKAGE_NAME = "packageName";
	private static final String TAG_SEVERITY = "severity";
	private static final String TAG_TYPE = "type";

	// See edu.hm.hafner.analysis.Severity
	private static final String SEVERITY_WARNING_LOW = "WARNING_LOW";
	private static final String SEVERITY_WARNING_NORMAL = "WARNING_NORMAL";
	private static final String SEVERITY_ERROR = "ERROR";

	// Custom category name for the resolver errors
	private static final String CATEGORY_RESOLVER_ERROR = "RESOLVER_ERROR";

	private final String componentID;
	private final ApiAnalysisResult result;
	private final Path baseDir;
	private final Path outFile;

	public ApiAnalysisXmlGenerator(String componentID, ApiAnalysisResult result, Path baseDir, Path outFile) {
		this.componentID = componentID;
		this.result = result;
		this.baseDir = baseDir;
		this.outFile = outFile;
	}

	public void writeReport() throws DOMException, IOException, ParserConfigurationException, TransformerException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.newDocument();
		document.setXmlStandalone(true);

		fillDocument(document);

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		DOMSource source = new DOMSource(document);

		Files.createDirectories(outFile.getParent());
		try (OutputStream outputStream = Files.newOutputStream(outFile)) {
			transformer.transform(source, new StreamResult(outputStream));
		}
	}

	private void fillDocument(Document document) {
		Element report = document.createElement(TAG_REPORT);
		document.appendChild(report);

		result.problems().forEach(problem -> addIssueElement(document, report, problem));

		if (result.resolveErrors().findAny().isPresent()) {
			result.resolveErrors().forEach(resolverError -> addIssueElement(document, report, resolverError));
		}
	}

	private void addIssueElement(Document document, Element parent, IApiProblem problem) {
		Element issue = document.createElement(TAG_ISSUE);

		addTextElement(document, issue, TAG_MODULE_NAME, componentID);
		addTextElement(document, issue, TAG_MESSAGE, problem.getMessage());
		addTextElement(document, issue, TAG_CATEGORY, ApiProblem.getProblemCategory(problem.getCategory()));
		addTextElement(document, issue, TAG_TYPE, ApiProblem.getProblemKind(problem.getCategory(), problem.getKind()));
		addTextElement(document, issue, TAG_SEVERITY, switch (problem.getSeverity()) {
			case ApiPlugin.SEVERITY_ERROR -> SEVERITY_ERROR;
			case ApiPlugin.SEVERITY_WARNING -> SEVERITY_WARNING_NORMAL;
			case ApiPlugin.SEVERITY_IGNORE -> SEVERITY_WARNING_LOW;
			default -> null;
		});
		int lineNumber = problem.getLineNumber();
		if (lineNumber >= 0) {
			addTextElement(document, issue, TAG_LINE_START, String.valueOf(lineNumber));
			addTextElement(document, issue, TAG_LINE_END, String.valueOf(lineNumber));
		}
		int charStart = problem.getCharStart();
		if (charStart >= 0) {
			addTextElement(document, issue, TAG_COLUMN_START, String.valueOf(charStart));
		}
		int charEnd = problem.getCharEnd();
		if (charEnd >= 0) {
			addTextElement(document, issue, TAG_COLUMN_END, String.valueOf(charEnd));
		}
		String path = String.valueOf(problem.getResourcePath());
		if (path != null) {
			addTextElement(document, issue, TAG_FILE_NAME, baseDir.resolve(Path.of(path)).toString());
		}
		String typeName = problem.getTypeName();
		if (typeName != null) {
			int lastDot = typeName.lastIndexOf('.');
			String packageName = lastDot < 0 ? typeName : typeName.substring(0, lastDot);
			addTextElement(document, issue, TAG_PACKAGE_NAME, packageName);
		}
		// additionalAttributes is considered in warnings-ng's equals() implementation
		// put the problem's ID there, consisting of
		// category | element kind | message | flags | kind
		// numerics
		addTextElement(document, issue, TAG_ADDITIONAL_ATTRIBUTES, "id=" + Integer.toString(problem.getId()));

		parent.appendChild(issue);
	}

	private void addIssueElement(Document document, Element parent, ResolverError resolverError) {
		Element issue = document.createElement(TAG_ISSUE);

		addTextElement(document, issue, TAG_MODULE_NAME, componentID);
		addTextElement(document, issue, TAG_MESSAGE, resolverError.toString());
		addTextElement(document, issue, TAG_CATEGORY, CATEGORY_RESOLVER_ERROR);

		parent.appendChild(issue);
	}

	private void addTextElement(Document document, Element issue, String elementName, String content) {
		if (content != null) {
			Element element = document.createElement(elementName);
			issue.appendChild(element);
			Text textNode = document.createTextNode(content);
			element.appendChild(textNode);
		}
	}

}
