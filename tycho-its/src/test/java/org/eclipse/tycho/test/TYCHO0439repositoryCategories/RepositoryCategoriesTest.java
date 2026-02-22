/*******************************************************************************
 * Copyright (c) 2008, 2024 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.TYCHO0439repositoryCategories;

import java.io.File;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;

public class RepositoryCategoriesTest extends AbstractTychoIntegrationTest {

	@Test
	public void testDeployableFeature() throws Exception {
		Verifier v01 = getVerifier("TYCHO0439repositoryCategories");
		v01.executeGoal("install");
		v01.verifyErrorFreeLog();

		File site = new File(v01.getBasedir(), "target/site");
		Assert.assertTrue(site.isDirectory());

		File content = new File(site, "content.jar");
		Assert.assertTrue(content.getAbsolutePath() + " is not a file!", content.isFile());

		boolean found = false;

		Document document = null;
		try (ZipFile contentJar = new ZipFile(content)) {
			ZipEntry contentXmlEntry = contentJar.getEntry("content.xml");
			document = Document.of(contentJar.getInputStream(contentXmlEntry));
		}
		Element repository = document.root();
		all_units: for (Element unit : repository.child("units").orElse(null).children("unit").toList()) {
			for (Element property : unit.child("properties").orElse(null).children("property").toList()) {
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
