/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.test.surefire;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class ParallelTestExecutionTest extends AbstractTychoIntegrationTest {

    @Test
    public void testParallelExecution() throws Exception {
        Verifier verifier = getVerifier("/surefire.junit47/parallel");
        verifier.getSystemProperties().setProperty("parallel", "classes");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("Concurrency config is parallel='classes', perCoreThreadCount=true, threadCount=3, useUnlimitedThreads=false");
        File surefireReportsDir = new File(verifier.getBasedir(), "target/surefire-reports");
        assertTrue(surefireReportsDir.isDirectory());
        File[] surefireXmlReports = surefireReportsDir.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.startsWith("TEST-") && name.endsWith(".xml");
            }
        });
        assertEquals(2, surefireXmlReports.length);
        Set<String> actualTests = extractExecutedTests(surefireXmlReports);
        Set<String> expectedTests = new HashSet<String>(asList("org.eclipse.tychoits.FirstTest#firstTest",
                "org.eclipse.tychoits.SecondTest#secondTest"));
        assertEquals(expectedTests, actualTests);
    }

    private Set<String> extractExecutedTests(File[] xmlReports) throws FileNotFoundException, XPathExpressionException,
            IOException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        Set<String> actualTests = new HashSet<String>();
        for (File xmlReportFile : xmlReports) {
            FileInputStream xmlStream = new FileInputStream(xmlReportFile);
            NodeList testCaseNodes;
            try {
                testCaseNodes = (NodeList) xpath.evaluate("/testsuite/testcase", new InputSource(xmlStream),
                        XPathConstants.NODESET);
            } finally {
                xmlStream.close();
            }
            for (int i = 0; i < testCaseNodes.getLength(); i++) {
                Element node = (Element) testCaseNodes.item(i);
                String testClassName = node.getAttribute("classname");
                String method = node.getAttribute("name");
                actualTests.add(testClassName + "#" + method);
            }
        }
        return actualTests;
    }

}
