/*******************************************************************************
 * Copyright (c) 2023 Red Hat, Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.p2.repository.FileBasedTychoRepositoryIndex;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.junit.Test;

public class IncludeAllSourcesTest extends AbstractTychoIntegrationTest {

	@Test
	public void testSourceInclusion() throws Exception {
		Verifier verifier = new IncludeAllSourcesTest().getVerifier("p2Repository.includeAllSources", false);
		verifier.executeGoal("verify");
		// Missing source should never trigger an error
		verifier.verifyErrorFreeLog();
		P2RepositoryTool p2Repo = P2RepositoryTool.forEclipseRepositoryModule(new File(verifier.getBasedir()));
		// testSourceInclusionDirectlyReferenced
		assertNotNull(p2Repo.getUniqueIU("org.apache.batik.css.source"));
		// testSourceInclusionTransitiveDeps
		assertNotNull(p2Repo.getUniqueIU("org.w3c.css.sac.source"));
		// testAllowMissingSource
		assertThrows(AssertionError.class, () -> {
			p2Repo.getUniqueIU("org.apache.commons.commons-io.source");
		});
	}

	@Test
	public void testIncludeAllSourcesFromOldOrbit() throws Exception {
		Verifier verifier = new IncludeAllSourcesTest().getVerifier("p2Repository.includeAllSources.oldOrbit", false);
		File localRepository = new File(verifier.getLocalRepository());
		File indexFile = new File(localRepository, FileBasedTychoRepositoryIndex.ARTIFACTS_INDEX_RELPATH);
		if (indexFile.exists()) {
			List<String> lines = Files.readAllLines(indexFile.toPath(), StandardCharsets.UTF_8);
			FileUtils.writeLines(indexFile, lines.stream().filter(line -> !line.contains("org.jdom.source")).toList());
		}
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		P2RepositoryTool p2Repo = P2RepositoryTool.forEclipseRepositoryModule(new File(verifier.getBasedir()));
		assertFalse(p2Repo.getUnitVersions("org.jdom.source").isEmpty(), "Missing org.jdom.source");
	}
}
