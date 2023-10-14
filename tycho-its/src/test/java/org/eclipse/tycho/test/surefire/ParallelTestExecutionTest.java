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
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.test.surefire;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.XMLTool;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ParallelTestExecutionTest extends AbstractTychoIntegrationTest {

	@Test
	public void testParallelExecution() throws Exception {
		Verifier verifier = getVerifier("/surefire.junit47/parallel");
		verifier.addCliOption("-Dparallel=classes");
		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();
		File surefireReportsDir = new File(verifier.getBasedir(), "target/surefire-reports");
		assertTrue(surefireReportsDir.isDirectory());
		File[] surefireXmlReports = surefireReportsDir
				.listFiles((FilenameFilter) (dir, name) -> name.startsWith("TEST-") && name.endsWith(".xml"));
		assertEquals(2, surefireXmlReports.length);
		Set<String> actualTests = extractExecutedTests(surefireXmlReports);
		Set<String> expectedTests = new HashSet<>(
				asList("org.eclipse.tychoits.FirstTest#firstTest", "org.eclipse.tychoits.SecondTest#secondTest"));
		assertEquals(expectedTests, actualTests);
	}

	private Set<String> extractExecutedTests(File[] xmlReports) throws Exception {
		Set<String> actualTests = new HashSet<>();
		for (File xmlReportFile : xmlReports) {
			Document document = XMLTool.parseXMLDocument(xmlReportFile);
			List<Node> matchingNodes = XMLTool.getMatchingNodes(document, "/testsuite/testcase");
			for (Node node : matchingNodes) {
				Element element = (Element) node;
				String testClassName = element.getAttribute("classname");
				String method = element.getAttribute("name");
				actualTests.add(testClassName + "#" + method);
			}
		}
		return actualTests;
	}

}
