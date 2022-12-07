/*******************************************************************************
 * Copyright (c) 2011, 2021 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.p2Inf;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipFile;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;

public class MultienvP2infTest extends AbstractTychoIntegrationTest {

	@Test
	public void test() throws Exception {
		Verifier verifier = getVerifier("/p2Inf.multiEnv", true);
		verifier.executeGoals(Arrays.asList("clean", "verify"));
		verifier.verifyErrorFreeLog();

		// assert repository contains cross-platform IUs defined in p2.inf files
		Document doc;

		try (ZipFile zip = new ZipFile(new File(verifier.getBasedir(), "product/target/repository/content.jar"))) {
			InputStream is = zip.getInputStream(zip.getEntry("content.xml"));
			doc = new XMLParser().parse(new XMLIOSource(is));
		}

		List<String> ids = new ArrayList<>();
		Element units = doc.getChild("repository/units");
		for (Element unit : units.getChildren("unit")) {
			ids.add(unit.getAttributeValue("id"));
		}

		// disabled due to a limitation of BundlesAction
		// Assert.assertTrue(ids.contains("tychotest.bundle.macosx"));

		assertTrue(ids.contains("tychotest.feature.macosx"));
		assertTrue(ids.contains("tychotest.product.macosx"));
	}
}
