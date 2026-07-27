/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test;

import java.io.File;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.it.Verifier;
import org.junit.Assert;
import org.junit.Test;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;

/**
 * category-p2-metadata must also work when the update site path contains a space
 * (e.g. a Jenkins workspace named "My Build"). The mojo passes the site location
 * as an already-encoded URI string to URIUtil.fromString, which is documented for
 * unencoded strings, so a naive toURI() double-encodes the space (%20 -&gt; %2520)
 * and the publisher fails with "P2 publisher return code was 1".
 */
public class CategoryP2MetadataPathWithSpacesTest extends AbstractTychoIntegrationTest {

	@Test
	public void testCategoryWithSpaceInPath() throws Exception {
		Verifier verifier = getVerifier("categoryP2Metadata.pathWithSpaces");
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();

		File site = new File(verifier.getBasedir(), "target/site with space");
		Assert.assertTrue(site.getAbsolutePath() + " is not a directory!", site.isDirectory());

		File content = new File(site, "content.jar");
		Assert.assertTrue(content.getAbsolutePath() + " is not a file!", content.isFile());

		boolean found = false;
		Document document;
		try (ZipFile contentJar = new ZipFile(content)) {
			ZipEntry contentXmlEntry = contentJar.getEntry("content.xml");
			document = Document.of(contentJar.getInputStream(contentXmlEntry));
		}
		Element repository = document.root();
		all_units: for (Element unit : repository.childElement("units").orElse(null).childElements("unit").toList()) {
			for (Element property : unit.childElement("properties").orElse(null).childElements("property").toList()) {
				if ("org.eclipse.equinox.p2.type.category".equals(property.attribute("name"))
						&& Boolean.parseBoolean(property.attribute("value"))) {
					found = true;
					break all_units;
				}
			}
		}

		Assert.assertTrue("Custom category is missing: " + content.getAbsolutePath(), found);
	}

}
