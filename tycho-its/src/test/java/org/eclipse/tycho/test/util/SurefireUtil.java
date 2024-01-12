/*******************************************************************************
 * Copyright (c) 2012, 2023 SAP AG and others.
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
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

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
		File resultFile = getTestResultFile(baseDir, className);
		Document document = readDocument(resultFile);
		// surefire-test-report XML schema:
		// https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd
		String testCaseXPath = String.format("/testsuite/testcase[@classname='%s' and @name='%s']", className,
				methodName);
		List<Node> testCaseNodes2 = XMLTool.getMatchingNodes(document, testCaseXPath);
		assertEquals(resultFile.getAbsolutePath() + " with xpath " + testCaseXPath
				+ " does not match the number of iterations", iterations, testCaseNodes2.size());

		List<Node> failureNodes = XMLTool.getMatchingNodes(document, testCaseXPath + "/failure");
		assertEquals(0, failureNodes.size());

		List<Node> errorNodes = XMLTool.getMatchingNodes(document, testCaseXPath + "/error");
		assertEquals(0, errorNodes.size());

		List<Node> skippedNodes = XMLTool.getMatchingNodes(document, testCaseXPath + "/skipped");
		assertEquals(0, skippedNodes.size());
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
		Document document = readDocument(getTestResultFile(baseDir, className));
		Node numberOfTests = XMLTool.getFirstMatchingNode(document, attributeXPath);
		return Integer.parseInt(numberOfTests.getNodeValue());
	}

	private static Document readDocument(File sureFireTestReport) throws Exception {
		assertTrue(sureFireTestReport.isFile());
		return XMLTool.parseXMLDocument(sureFireTestReport);
	}

	private static File getTestResultFile(String baseDir, String className) {
		File sureFireTestReport = new File(baseDir, "target/surefire-reports/TEST-" + className + ".xml");
		return sureFireTestReport;
	}
}
