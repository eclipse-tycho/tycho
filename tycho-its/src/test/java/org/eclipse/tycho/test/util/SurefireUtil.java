/*******************************************************************************
 * Copyright (c) 2012, 2018 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class SurefireUtil {

	private static final String TEST_REPORT_PATH = "target/surefire-reports/TEST-";

	public static File testResultFile(String baseDir, String packageName, String className) {
		return new File(baseDir, TEST_REPORT_PATH + packageName + "." + className + ".xml");
	}

	public static File testResultFile(String baseDir, String testSuffix) {
		return new File(baseDir, TEST_REPORT_PATH + testSuffix + ".xml");
	}

	public static void assertTestMethodWasSuccessfullyExecuted(String baseDir, String className, String methodName,
			int iterations) throws Exception {
		Document document = readDocument(baseDir, className);
		XPath xpath = XPathFactory.newInstance().newXPath();
		// surefire-test-report XML schema:
		// https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd
		String testCaseXPath = String.format("/testsuite/testcase[@classname='%s' and @name='%s']", className,
				methodName);
		NodeList testCaseNodes = (NodeList) xpath.evaluate(testCaseXPath, document, XPathConstants.NODESET);
		assertEquals(iterations, testCaseNodes.getLength());

		NodeList failureNodes = (NodeList) xpath.evaluate(testCaseXPath + "/failure", document, XPathConstants.NODESET);
		assertEquals(0, failureNodes.getLength());

		NodeList errorNodes = (NodeList) xpath.evaluate(testCaseXPath + "/error", document, XPathConstants.NODESET);
		assertEquals(0, errorNodes.getLength());

		NodeList skippedNodes = (NodeList) xpath.evaluate(testCaseXPath + "/skipped", document, XPathConstants.NODESET);
		assertEquals(0, skippedNodes.getLength());
	}

	public static void assertTestMethodWasSuccessfullyExecuted(String baseDir, String className, String methodName)
			throws Exception {
		assertTestMethodWasSuccessfullyExecuted(baseDir, className, methodName, 1);
	}

	public static void assertNumberOfSuccessfulTests(String baseDir, String className,
			int expectedNumberOfSuccessfulTests) throws Exception {
		assertEquals(expectedNumberOfSuccessfulTests, extractNumericAttribute(baseDir, className, "/testsuite/@tests"));
	}

	public static void assertNumberOfFailedTests(String baseDir, String className, int expectedNumberOfFailedTests)
			throws Exception {
		assertEquals(expectedNumberOfFailedTests, extractNumericAttribute(baseDir, className, "/testsuite/@failures"));
	}

	public static void assertNumberOfErroneousTests(String baseDir, String className,
			int expectedNumberOfErroneousTests) throws Exception {
		assertEquals(expectedNumberOfErroneousTests, extractNumericAttribute(baseDir, className, "/testsuite/@errors"));
	}

	public static void assertNumberOfSkippedTests(String baseDir, String className, int expectedNumberOfSkippedTests)
			throws Exception {
		assertEquals(expectedNumberOfSkippedTests, extractNumericAttribute(baseDir, className, "/testsuite/@skipped"));
	}

	private static int extractNumericAttribute(String baseDir, String className, String attributeXPath)
			throws Exception {
		Document document = readDocument(baseDir, className);
		XPath xpath = XPathFactory.newInstance().newXPath();
		String numberOfTests = (String) xpath.evaluate(attributeXPath, document, XPathConstants.STRING);
		return Integer.parseInt(numberOfTests);
	}

	private static Document readDocument(String baseDir, String className) throws Exception {
		File sureFireTestReport = new File(baseDir, "target/surefire-reports/TEST-" + className + ".xml");
		assertTrue(sureFireTestReport.isFile());
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(sureFireTestReport);
		return document;
	}
}
