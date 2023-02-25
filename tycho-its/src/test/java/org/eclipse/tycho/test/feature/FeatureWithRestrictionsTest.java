/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.feature;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLParser;

public class FeatureWithRestrictionsTest extends AbstractTychoIntegrationTest {

	@Test
	public void testFeatureRestriction() throws Exception {
		Verifier verifier = getVerifier("feature.restrictions/sample.feature", false, true);
		verifier.setSystemProperty("repo-url", "file:"
				+ ResourceUtil.resolveTestResource("projects/feature.restrictions/repository").getAbsolutePath());
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File contentXml = new File(verifier.getBasedir(), "target/p2content.xml");
		Document artifactsDocument = XMLParser.parse(contentXml);
		String unitId = "com.test.sample.feature.feature.group";
		Optional<Element> unit = artifactsDocument.getChild("units").getChildren("unit").stream().filter(elem -> {
			return unitId.equals(elem.getAttributeValue("id"));
		}).findFirst();
		assertTrue("Unit with id " + unitId + " not found", unit.isPresent());
		assertFalse("Version 2 was required by unit",
				unit.stream().flatMap(elem -> elem.getChild("requires").getChildren("required").stream())
						.anyMatch(elem -> "[2.0.0,2.0.0]".equals(elem.getAttributeValue("range"))));
	}
}
