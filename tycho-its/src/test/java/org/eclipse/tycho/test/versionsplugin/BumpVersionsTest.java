/*******************************************************************************
 * Copyright (c) 2026 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.versionsplugin;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the bump-versions mojo from tycho-versions-plugin.
 *
 */
public class BumpVersionsTest extends AbstractTychoIntegrationTest {

	private static final String PROJECT = "tycho-version-plugin/bump-versions-update-packages";

	private static File baselineRepo = null;

	@Before
	public void buildBaselineRepository() throws Exception {
		if (baselineRepo == null) {
			Verifier verifier = getVerifier(PROJECT, false, true);
			verifier.addCliOption("-Dtycho.baseline.skip=true");
			verifier.executeGoals(List.of("clean", "package"));
			verifier.verifyErrorFreeLog();
			File repoDir = new File(verifier.getBasedir(), "repo/target/repository");
			assertTrue("Baseline p2 repository was not created at: " + repoDir.getAbsolutePath(),
					repoDir.isDirectory());
			baselineRepo = new File("target/projects", getClass().getSimpleName() + "/baselineRepo").getAbsoluteFile();
			if (baselineRepo.isDirectory()) {
				FileUtils.deleteDirectory(baselineRepo);
			}
			FileUtils.copyDirectoryStructure(repoDir, baselineRepo);
		}
	}

	/**
	 * When a MAJOR API change is made (adding a method to a public interface), bnd
	 * suggests a version bump for the package and the bundle. Since only one version
	 * bump can be reported at a time, two sequential failed builds are expected
	 * before the versions converge and the build succeeds:
	 * <ol>
	 *   <li>Run 1 (fails): bnd detects the MAJOR change; the bundle version is bumped
	 *       to 2.0.0 while the Export-Package version stays at 1.0.0.</li>
	 *   <li>Run 2 (fails): the bundle version is already correct; bnd now reports the
	 *       package version violation and bumps Export-Package to 2.0.0.</li>
	 *   <li>Run 3 (passes): both versions are consistent with the baseline.</li>
	 * </ol>
	 */
	@Test
	public void testBumpVersionsUpdatesExportedPackageVersions() throws Exception {
		Verifier verifier = getVerifier(PROJECT, false, true);
		Path bundleDir = Path.of(verifier.getBasedir(), "bundle");

		// Add a method to the public interface — bnd classifies this as a MAJOR delta,
		// requiring version 2.0.0 for both the exported package and the bundle.
		Files.writeString(bundleDir.resolve("src/tycho/its/bump/versions/MyInterface.java"),
				"package tycho.its.bump.versions;\n\npublic interface MyInterface {\n    void newMethod();\n}\n");

		verifier.setAutoclean(false);
		verifier.addCliOption("-f bundle/pom.xml");
		verifier.addCliOption("-Dbaseline-url=" + baselineRepo.toURI());

		// === Run 1: bundle version bumped; Export-Package stays at baseline version ===
		assertThrows(VerificationException.class, () -> verifier.executeGoals(List.of("clean", "verify")));

		verifier.verifyTextInLog("requires a version bump");

		// Read MANIFEST.MF before run 2 overwrites the log
		String manifestAfterRun1 = Files.readString(bundleDir.resolve("META-INF/MANIFEST.MF"));

		assertTrue("Bundle version should be bumped to 2.0.0 after run 1:\n" + manifestAfterRun1,
				manifestAfterRun1.contains("Bundle-Version: 2.0.0"));
		assertTrue("Export-Package version should still be 1.0.0 after run 1:\n" + manifestAfterRun1,
				manifestAfterRun1.contains("Export-Package: tycho.its.bump.versions;version=\"1.0.0\""));

		// === Run 2: Export-Package version bumped to match the bundle ===
		assertThrows(VerificationException.class, () -> verifier.executeGoals(List.of("verify")));

		String manifestAfterRun2 = Files.readString(bundleDir.resolve("META-INF/MANIFEST.MF"));

		assertTrue("Bundle version should remain 2.0.0 after run 2:\n" + manifestAfterRun2,
				manifestAfterRun2.contains("Bundle-Version: 2.0.0"));
		assertTrue("Export-Package version should be bumped to 2.0.0 after run 2:\n" + manifestAfterRun2,
				manifestAfterRun2.contains("Export-Package: tycho.its.bump.versions;version=\"2.0.0\""));

		// === Run 3: both versions consistent, build must succeed ===
		verifier.executeGoals(List.of("verify"));
		verifier.verifyErrorFreeLog();
	}
}
