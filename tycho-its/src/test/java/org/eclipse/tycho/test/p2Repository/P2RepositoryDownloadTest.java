/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.p2.repository.FileBasedTychoRepositoryIndex;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLParser;

public class P2RepositoryDownloadTest extends AbstractTychoIntegrationTest {

	@Test
	public void testDownloadOnlyOnce() throws Exception {
		Verifier verifier = getVerifier("p2Repository.duplicateDownload", false, true);
		verifier.displayStreamBuffers();
		File localRepository = new File(verifier.getLocalRepository());
		File indexFile = new File(localRepository, FileBasedTychoRepositoryIndex.ARTIFACTS_INDEX_RELPATH);
		String[] bundles = { "org.eclipse.swt", "com.google.guava" };
		if (indexFile.exists()) {
			List<String> lines = Files.readAllLines(indexFile.toPath(), StandardCharsets.UTF_8);
			FileUtils.writeLines(indexFile, lines.stream().filter(line -> {
				for (String bundle : bundles) {
					if (line.contains("p2.osgi.bundle:" + bundle)) {
						return false;
					}
				}
				return true;
			}).toList());
		}
		// first pass must download...
		verifier.executeGoals(List.of("clean", "install"));
		verifier.verifyErrorFreeLog();
		for (String bundle : bundles) {
			verifier.verifyTextInLog("Writing P2 metadata for osgi.bundle," + bundle);
		}
		// second run should *not* download!
		verifier.executeGoals(List.of("clean", "install"));
		verifier.verifyErrorFreeLog();
		for (String bundle : bundles) {
			try {
				verifier.verifyTextInLog("Writing P2 metadata for osgi.bundle," + bundle);
				fail(bundle + " is fetched twice!");
			} catch (VerificationException e) {
				assertTrue(e.getMessage().contains("Text not found"));
			}
		}
	}

	@Test
	public void testReactorCanBeVerified() throws Exception {
		Verifier verifier = getVerifier("reactor.makeBehaviour", true, true);
		verifier.addCliOption("-T1C");
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
		verifyTextNotInLog(verifier, "No digest algorithm is available to verify download of");
		verifyHasChecksum(new File(verifier.getBasedir(), "feature3/target/p2artifacts.xml"));
		verifyHasChecksum(new File(verifier.getBasedir(), "bundle1/target/p2artifacts.xml"));
	}

	void verifyHasChecksum(File artifactXml) throws IOException {
		assertTrue("required artifact file " + artifactXml.getAbsolutePath() + " not found!", artifactXml.exists());
		Document artifactsDocument = XMLParser.parse(artifactXml);
		for (Element artifact : artifactsDocument.getChild("artifacts").getChildren("artifact")) {
			Map<String, String> map = artifact.getChild("properties").getChildren("property").stream()
					.collect(Collectors.toMap(e -> e.getAttributeValue("name"), e -> e.getAttributeValue("value")));
			if (!map.containsKey("download.checksum.sha-256")) {
				fail("Checksum property not found for artifact: \r\n" + artifact);
			}
		}
	}
}
