/*******************************************************************************
 * Copyright (c) 2022, 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *     Hannes Wellman - add verify test case
 *******************************************************************************/
package org.eclipse.tycho.test.apitools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.XMLParser;

public class ApiToolsTest extends AbstractTychoIntegrationTest {
	@Test
	public void testGenerate() throws Exception {
		Verifier verifier = getVerifier("api-tools", true, true);
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File descriptionFile = new File(verifier.getBasedir(), "bundle1/target/.api_description");
		assertTrue(descriptionFile.getAbsoluteFile() + " not found", descriptionFile.isFile());
		Document document = XMLParser.parse(descriptionFile);
		assertEquals("api-bundle-1_0.0.1-SNAPSHOT", document.getRootElement().getAttribute("name").getValue());
		// TODO enhance project and assert more useful things...
	}

	@Test
	public void testVerify() throws Exception {
		Verifier verifier = getVerifier("api-tools", true, true);
		File repo = ResourceUtil.resolveTestResource("repositories/api-tools");
		verifier.addCliOption("-DbaselineRepo=" + repo.toURI());

		assertThrows("No API errors where detected!", VerificationException.class,
				() -> verifier.executeGoals(List.of("clean", "verify")));

		verifier.verifyTextInLog("4 API ERRORS");
		verifier.verifyTextInLog("0 API warnings");
		verifier.verifyTextInLog("The type bundle.ApiInterface has been removed from api-bundle");
		verifier.verifyTextInLog("The type bundle.InterfaceA has been removed from api-bundle");
		verifier.verifyTextInLog("The type bundle.ClassA has been removed from api-bundle");
		verifier.verifyTextInLog(
				"The major version should be incremented in version 0.0.1, since API breakage occurred since version 0.0.1");

		// TODO: check with api-filter
		// TODO: check with second plugin with BREE?
	}
}
