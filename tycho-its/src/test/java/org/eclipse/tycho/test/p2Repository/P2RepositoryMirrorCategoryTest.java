/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Contributors to the Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Verifies that the {@code mirror} goal of the tycho-p2-extras-plugin injects a
 * parent category that wraps all categories of the mirrored source repository
 * when {@code categoryName} is configured.
 */
public class P2RepositoryMirrorCategoryTest extends AbstractTychoIntegrationTest {

	private static final String PARENT_CATEGORY_ID = "tycho.mirroring.category.parent.category";

	@Test
	public void testParentCategoryInjected() throws Exception {
		Verifier verifier = getVerifier("p2Repository.mirror.category", false);

		// Point the configured source repository at the bundled fixture
		File sourceRepo = new File(verifier.getBasedir(), "source-repo");
		verifier.addCliOption("-Dsource-url=" + sourceRepo.toURI());

		verifier.executeGoals(List.of("verify"));
		verifier.verifyErrorFreeLog();
		verifier.verifyTextInLog("Injecting parent category \"Parent Category\"");

		Document content = P2RepositoryMirrorTest
				.parseDocument(new File(verifier.getBasedir(), "target/repository/content.xml"));
		XPath xpath = XPathFactory.newInstance().newXPath();

		String parentExpr = "repository/units/unit[@id='" + PARENT_CATEGORY_ID + "']";
		Node parent = (Node) xpath.evaluate(parentExpr, content, XPathConstants.NODE);
		assertNotNull("Parent category IU was not injected into the mirrored repository", parent);

		assertEquals("Parent IU must be marked as a category", "true", xpath.evaluate(
				parentExpr + "/properties/property[@name='org.eclipse.equinox.p2.type.category']/@value", content));

		NodeList requiredNames = (NodeList) xpath.evaluate(parentExpr + "/requires/required/@name", content,
				XPathConstants.NODESET);
		Set<String> requiredIds = new HashSet<>();
		for (int i = 0; i < requiredNames.getLength(); i++) {
			requiredIds.add(requiredNames.item(i).getNodeValue());
		}

		assertTrue("Parent should wrap category alpha", requiredIds.contains("org.eclipse.example.category.alpha"));
		assertTrue("Parent should wrap category beta", requiredIds.contains("org.eclipse.example.category.beta"));
		assertEquals("Parent should wrap exactly the two source categories", 2, requiredIds.size());
	}
}
