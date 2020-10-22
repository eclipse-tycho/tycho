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
import static org.junit.Assert.assertNotNull;
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

    public static void assertTestMethodWasSuccessfullyExecuted(String baseDir, String className, String methodName)
            throws Exception {
        File sureFireTestReport = new File(baseDir, "target/surefire-reports/TEST-" + className + ".xml");
        assertTrue(sureFireTestReport.isFile());
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(sureFireTestReport);
        XPath xpath = XPathFactory.newInstance().newXPath();
        // surefire-test-report XML schema: https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd
        NodeList testCaseNodes = (NodeList) xpath.evaluate(
                String.format("/testsuite/testcase[@classname='%s' and @name='%s']", className, methodName), document,
                XPathConstants.NODESET);
        assertNotNull(testCaseNodes);
        assertEquals(1, testCaseNodes.getLength());
        NodeList failureNodes = (NodeList) xpath.evaluate(
                String.format("/testsuite/testcase[@classname='%s' and @name='%s']/failure", className, methodName),
                document, XPathConstants.NODESET);
        assertEquals(0, failureNodes.getLength());
        NodeList errorNodes = (NodeList) xpath.evaluate(
                String.format("/testsuite/testcase[@classname='%s' and @name='%s']/error", className, methodName),
                document, XPathConstants.NODESET);
        assertEquals(0, errorNodes.getLength());
    }

    public static void assertNumberOfSuccessfulTests(String baseDir, String className,
            int expectedNumberOfSuccessfulTests) throws Exception {
        File sureFireTestReport = new File(baseDir, "target/surefire-reports/TEST-" + className + ".xml");
        assertTrue(sureFireTestReport.isFile());
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(sureFireTestReport);
        XPath xpath = XPathFactory.newInstance().newXPath();
        String numberOfTests = (String) xpath.evaluate("/testsuite/@tests", document, XPathConstants.STRING);
        assertEquals(expectedNumberOfSuccessfulTests, Integer.parseInt(numberOfTests));
    }
}
