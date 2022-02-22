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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.repository.local.index.FileBasedTychoRepositoryIndex;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class P2RepositoryDownloadTest extends AbstractTychoIntegrationTest {

	@Test
	public void testDownloadOnlyOnce() throws Exception {
		Verifier verifier = getVerifier("p2Repository.duplicateDownload", false, true);
		File localRepository = new File(verifier.getLocalRepository());
		File indexFile = new File(localRepository, FileBasedTychoRepositoryIndex.ARTIFACTS_INDEX_RELPATH);
		if (indexFile.exists()) {
			List<String> lines = FileUtils.readLines(indexFile, StandardCharsets.UTF_8);
			FileUtils.writeLines(indexFile, lines.stream()
					.filter(line -> !line.contains("p2.osgi.bundle:com.google.guava:30")).collect(Collectors.toList()));
		}
		// first pass must download...
		verifier.executeGoals(List.of("clean", "install"));
		verifier.verifyErrorFreeLog();
		verifier.verifyTextInLog("Writing P2 metadata for osgi.bundle,com.google.guava");
		// second run should *not* download!
		verifier.executeGoals(List.of("clean", "install"));
		verifier.verifyErrorFreeLog();
		try {
			verifier.verifyTextInLog("Writing P2 metadata for osgi.bundle,com.google.guava");
			fail("guava is fetched twice!");
		} catch (VerificationException e) {
			assertTrue(e.getMessage().contains("Text not found"));
		}
	}
}
