/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.reproducible;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class ReproducibleBuildTest extends AbstractTychoIntegrationTest {
	// The ZipEntry.getLastModifiedTime() method uses the default timezone to
	// convert date and time fields to Instant, so we also use the default timezone
	// for the expected timestamp here.
	private static final String EXPECTED_TIMESTAMP_STRING = "2023-01-01T00:00:00";
	private static final Instant EXPECTED_TIMESTAMP_INSTANT = LocalDateTime.parse(EXPECTED_TIMESTAMP_STRING)
			.toInstant(OffsetDateTime.now().getOffset());

	/**
	 * Check that the build is reproducible.
	 */
	@Test
	public void test() throws Exception {
		Verifier verifier = getVerifier("reproducible-build");
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();

		// Check that the timestamp of the files inside the produced archives is equal
		// to the one specified in the "project.build.outputTimestamp" property of the
		// pom file.
		checkTimestamps(verifier.getBasedir() + "/reproducible.bundle/target/reproducible.bundle-1.0.0.jar");
		checkTimestamps(verifier.getBasedir() + "/reproducible.bundle/target/reproducible.bundle-1.0.0-attached.jar");
		checkTimestamps(verifier.getBasedir() + "/reproducible.bundle/target/reproducible.bundle-1.0.0-sources.jar");
		checkTimestamps(
				verifier.getBasedir() + "/reproducible.bundle.feature/target/reproducible.bundle.feature-1.0.0.jar");
		checkTimestamps(verifier.getBasedir()
				+ "/reproducible.bundle.feature/target/reproducible.bundle.feature-1.0.0-sources-feature.jar");
		checkTimestamps(verifier.getBasedir() + "/reproducible.iu/target/reproducible.iu-1.0.0.zip");
		checkTimestamps(verifier.getBasedir() + "/reproducible.repository/target/reproducible.repository-1.0.0.zip");
		checkTimestamps(verifier.getBasedir() + "/reproducible.repository/target/p2-site.zip");

		// Check that the build qualifier uses the timestamp specified in the
		// "project.build.outputTimestamp" property of the pom file.
		checkBuildQualifier(verifier.getBasedir()
				+ "/reproducible.buildqualifier/target/reproducible.buildqualifier-1.0.0-SNAPSHOT.jar");
	}

	private void checkTimestamps(String file) throws IOException {
		try (ZipFile zip = new ZipFile(file)) {
			final var entries = zip.entries();
			while (entries.hasMoreElements()) {
				final ZipEntry entry = entries.nextElement();
				Assert.assertEquals(EXPECTED_TIMESTAMP_INSTANT, entry.getLastModifiedTime().toInstant());
			}
		}
	}

	private void checkBuildQualifier(String file) throws IOException {
		try (FileSystem fileSystem = FileSystems.newFileSystem(Path.of(file))) {
			Path manifest = fileSystem.getPath("META-INF/MANIFEST.MF");
			List<String> lines = Files.readAllLines(manifest);
			Assert.assertTrue(lines.stream().anyMatch(l -> l.equals("Bundle-Version: 1.0.0.202301010000")));
		}
	}
}
