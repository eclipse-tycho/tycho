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
 *******************************************************************************/
package org.eclipse.tycho.test.buildextension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.jar.JarFile;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;
import org.osgi.framework.Constants;

public class CiFriendlyVersionsTest extends AbstractTychoIntegrationTest {

	@Test
	public void testDefaultBuildQualifier() throws Exception {
		Verifier verifier = getVerifier("ci-friendly/buildqualifier", false, true);
		// this used the default build qualifier
		verifier.addCliOption("-Dtycho.buildqualifier.format=yyyy");
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		int year = Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.YEAR);
		File file = new File(verifier.getBasedir(), "bundle/target/bundle-1.0.0." + year + ".jar");
		assertTrue(file.getAbsolutePath() + " is not generated!", file.isFile());
		checkManifestVersion(file, "1.0.0." + year);
	}

	@Test
	public void testJgitBuildQualifier() throws Exception {
		Verifier verifier = getVerifier("ci-friendly/buildqualifier", false, true);
		// this used the default build qualifier
		verifier.addCliOption("-Dtycho.buildqualifier.provider=jgit");
		verifier.addCliOption("-Dtycho.buildqualifier.format=yyyyMM");
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
		File targetDir = new File(verifier.getBasedir(), "bundle/target");
		String[] jarFiles = targetDir.list((dir, name) -> name.endsWith(".jar"));
		assertEquals(1, jarFiles.length);
		File file = new File(targetDir, jarFiles[0]);
		assertTrue(file.getAbsolutePath() + " is not generated!", file.isFile());
		String qualifier = jarFiles[0].substring(13, jarFiles[0].lastIndexOf('.'));
		// if formatter fails to parse it will throw exception and thus fail the test
		formatter.parse(qualifier);
		checkManifestVersion(file, "1.0.0." + qualifier);
	}

	@Test
	public void testForcedBuildQualifier() throws Exception {
		Verifier verifier = getVerifier("ci-friendly/buildqualifier", false, true);
		// this uses a forced qualifier
		verifier.addCliOption("-DforceContextQualifier=abc");
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File file = new File(verifier.getBasedir(), "bundle/target/bundle-1.0.0.abc.jar");
		assertTrue(file.getAbsolutePath() + " is not generated!", file.isFile());
		checkManifestVersion(file, "1.0.0.abc");
	}

	@Test
	public void testWithSnapshotBuildQualifier() throws Exception {
		// building with nothing should result in the default -SNAPSHOT build
		Verifier verifier = getVerifier("ci-friendly/buildqualifier", false, true);
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File file = new File(verifier.getBasedir(), "bundle/target/bundle-1.0.0-SNAPSHOT.jar");
		assertTrue(file.getAbsolutePath() + " is not generated!", file.isFile());
	}

	@Test
	public void testMilestoneBuildQualifier() throws Exception {
		Verifier verifier = getVerifier("ci-friendly/buildqualifier", false, true);
		// this used the default build qualifier
		verifier.addCliOption("-Dqualifier=-M1");
		verifier.addCliOption("-DforceContextQualifier=zzz");
		verifier.addCliOption("-Dtycho.strictVersions=false");
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File file = new File(verifier.getBasedir(), "bundle/target/bundle-1.0.0-M1.jar");
		assertTrue(file.getAbsolutePath() + " is not generated!", file.isFile());
		checkManifestVersion(file, "1.0.0.zzz");
	}

	@Test
	public void testReleaseBuildWithForcedContextQualifier() throws Exception {
		Verifier verifier = getVerifier("ci-friendly/buildqualifier", false, true);
		// this uses force context qualifier set to none
		verifier.addCliOption("-DforceContextQualifier=none");
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File file = new File(verifier.getBasedir(), "bundle/target/bundle-1.0.0.jar");
		assertTrue(file.getAbsolutePath() + " is not generated!", file.isFile());
		checkManifestVersion(file, "1.0.0");
	}

	private static void checkManifestVersion(File file, String expectedVersion) throws IOException {
		try (JarFile jarFile = new JarFile(file)) {
			assertEquals(expectedVersion, jarFile.getManifest().getMainAttributes().getValue(Constants.BUNDLE_VERSION));
		}
	}
}
