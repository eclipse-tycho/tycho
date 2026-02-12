/*******************************************************************************
 * Copyright (c) 2023, 2023 Hannes Wellmann and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.feature;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.XMLTool;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

public class FeatureAttributesInferenceTest extends AbstractTychoIntegrationTest {

	@Test
	public void testFeatureAttributesInference() throws Exception {
		Verifier verifier = getVerifier("feature.attributes.inference", true, true);
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File featureTargetDir = new File(verifier.getBasedir(), "feature/target");
		File featureJar = assertFileExists(featureTargetDir, "feature.attributes.inference.test-1.0.0.jar")[0];

		List<Element> pluginNodes = getPluginElements(featureJar);
		Assert.assertEquals(3, pluginNodes.size());

		// Check the feature.xml in the feature-jar
		assertAttributesOnlyElementWith(Map.of(//
				"id", equalTo("junit-jupiter-api"), //
				"version", isSpecificVersion() //
		), pluginNodes.get(0));

		assertAttributesOnlyElementWith(Map.of(//
				"id", equalTo("junit-platform-suite-api"), //
				"version", isSpecificVersion() //
		), pluginNodes.get(1));

		assertAttributesOnlyElementWith(Map.of(//
				"id", equalTo("org.apiguardian.api"), //
				"version", isSpecificVersion() //
		), pluginNodes.get(2));
	}

	private List<Element> getPluginElements(File featureJar) throws Exception {
		Document document = XMLTool.parseXMLDocumentFromJar(featureJar, "feature.xml");
		return XMLTool.getMatchingNodes(document, "/feature/plugin").stream().filter(Element.class::isInstance)
				.map(Element.class::cast).toList();
	}

	private static Matcher<String> isSpecificVersion() {
		return not(anyOf(blankOrNullString(), equalTo("0.0.0")));
	}

	private void assertAttributesOnlyElementWith(Map<String, Matcher<String>> expectedAttributes, Element element) {
		assertEquals(0, element.getChildNodes().getLength());
		NamedNodeMap attributes = element.getAttributes();
		Map<String, String> elementAttributes = IntStream.range(0, attributes.getLength()).mapToObj(attributes::item)
				.map(Attr.class::cast).collect(Collectors.toMap(Attr::getName, Attr::getValue));

		expectedAttributes.forEach((name, expectation) -> assertThat(elementAttributes.get(name), expectation));
		assertEquals(expectedAttributes.size(), elementAttributes.size());
	}

}
