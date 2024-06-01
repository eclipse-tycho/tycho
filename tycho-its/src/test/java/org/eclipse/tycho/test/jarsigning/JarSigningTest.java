/*******************************************************************************
 * Copyright (c) 2011, 2023 SAP AG
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.jarsigning;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.XMLTool;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class JarSigningTest extends AbstractTychoIntegrationTest {

	@Test
	public void testSigning() throws Exception {
		Verifier verifier = getVerifier("jar-signing", true);

		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		verifier.verifyTextInLog("verified successfully");
		checkSha256SumsArePresent(verifier);
	}

	@Test
	public void testExtraSigning() throws Exception {
		Verifier verifier = getVerifier("jar-signing-extra", true);

		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		verifier.verifyTextInLog("verified successfully");
		checkSha256SumsArePresent(verifier);
	}

	private void checkSha256SumsArePresent(Verifier verifier) throws Exception {
		File repoDir = new File(verifier.getBasedir(), "rcp/target/repository");
		File artifacts = new File(repoDir, "artifacts.jar");
		assertTrue(artifacts.isFile());
		Document document = XMLTool.parseXMLDocumentFromJar(artifacts, "artifacts.xml");
		List<Node> artifactNodes = XMLTool.getMatchingNodes(document, "/repository/artifacts/artifact");
		for (Node artifact : artifactNodes) {
			List<Node> checksumProperties = XMLTool.getMatchingNodes(artifact,
					"properties/property[@name='download.checksum.sha-256']");
			assertFalse("artifact does not have a 'download.checksum.sha-256' attribute", checksumProperties.isEmpty());
		}
	}
}
