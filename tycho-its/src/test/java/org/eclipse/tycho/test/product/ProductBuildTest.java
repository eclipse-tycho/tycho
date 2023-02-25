/*******************************************************************************
 * Copyright (c) 2022 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.product;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLParser;

public class ProductBuildTest extends AbstractTychoIntegrationTest {

	private static final List<String> REQUIRED_PGP_PROPERTIES = List.of(TychoConstants.PROP_PGP_SIGNATURES,
			TychoConstants.PROP_PGP_KEYS);

	@Test
	public void testMavenDepedencyInTarget() throws Exception {
		Verifier verifier = getVerifier("product.mavenLocation", false);
		verifier.executeGoals(Arrays.asList("clean", "verify"));
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testPGPSignedInProduct() throws Exception {
		Verifier verifier = getVerifier("product.pgp", false);
		verifier.executeGoals(Arrays.asList("clean", "install"));
		verifier.verifyErrorFreeLog();
		checkPGP(verifier, "target/repository/artifacts.xml");
		checkPGP(verifier, "target/products/pgp/linux/gtk/x86_64/artifacts.xml");

	}

	protected void checkPGP(Verifier verifier, String repositoryArtifacts) throws IOException {
		File artifactXml = new File(verifier.getBasedir(), repositoryArtifacts);
		assertTrue("required artifacts file " + artifactXml.getAbsolutePath() + " not found!", artifactXml.isFile());
		Document artifactsDocument = XMLParser.parse(artifactXml);
		Optional<Element> optional = artifactsDocument.getChild("repository").getChild("artifacts")
				.getChildren("artifact").stream()
				.filter(element -> element.getAttributeValue("id").equals("org.mockito.mockito-core")).findAny();
		assertTrue("artifact org.mockito.mockito-core not found", optional.isPresent());
		Element element = optional.get();
		Map<String, String> properties = element.getChild("properties").getChildren("property").stream()
				.collect(Collectors.toMap(e -> e.getAttributeValue("name"), e -> e.getAttributeValue("value")));
		for (String property : REQUIRED_PGP_PROPERTIES) {
			assertTrue("property " + property + " is missing", properties.containsKey(property));
			assertFalse("property " + property + " is present but empty", properties.get(property).isBlank());

		}
	}
}
