/*******************************************************************************
 * Copyright (c) 2026 Christopher Lake and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christopher Lake - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.util.List;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.XMLTool;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Tests that features included with <code>optional="true"</code> do not have to be available in the target platform.
 */
public class FeatureOptionalIncludesTest extends AbstractTychoIntegrationTest {

	@Test
	public void testMissingOptionalIncludeIsKept() throws Exception {
		Verifier verifier = getVerifier("feature.optionalIncludes", false);
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();

		File featureJar = assertFileExists(new File(verifier.getBasedir(), "main/target"),
				"feature.optionalIncludes.main-1.0.0-SNAPSHOT.jar")[0];
		Document document = XMLTool.parseXMLDocumentFromJar(featureJar, "feature.xml");
		List<Element> includes = XMLTool.getMatchingNodes(document, "/feature/includes").stream()
				.filter(Element.class::isInstance).map(Element.class::cast).toList();
		assertEquals(2, includes.size());

		Element included = includes.get(0);
		assertEquals("feature.optionalIncludes.included", included.getAttribute("id"));
		assertNotEquals("0.0.0", included.getAttribute("version"), "version of the resolved include is expanded");

		Element missing = includes.get(1);
		assertEquals("feature.optionalIncludes.missing", missing.getAttribute("id"));
		assertEquals("0.0.0", missing.getAttribute("version"), "unresolved optional include is left unchanged");
		assertEquals("true", missing.getAttribute("optional"));
	}

	@Test
	public void testMissingRequiredIncludeFails() throws Exception {
		Verifier verifier = getVerifier("feature.optionalIncludes.required", false);
		assertThrows(VerificationException.class, () -> verifier.executeGoals(List.of("clean", "package")));
		verifier.verifyTextInLog("feature.optionalIncludes.missing");
	}
}
