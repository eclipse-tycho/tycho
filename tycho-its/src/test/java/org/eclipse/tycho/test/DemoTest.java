/*******************************************************************************
 * Copyright (c) 2023, 2026 Christoph Läubrich and others.
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
package org.eclipse.tycho.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.jar.JarFile;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * This integration test builds and tests the demo projects we provide in the
 * repository
 */
public class DemoTest extends AbstractTychoIntegrationTest {

	@Test
	public void testSignExecutableDemo() throws Exception {
		Verifier verifier = runDemo("custom-signing-product");
		Path basedir = Path.of(verifier.getBasedir());
		// Check the product only contains our "modified" launcher
		Path exe = basedir.resolve("target/products/launchericons/win32/win32/x86_64/eclipse.exe");
		Path console = basedir.resolve("target/products/launchericons/win32/win32/x86_64/eclipsec.exe");
		assertTrue(Files.isRegularFile(exe), exe + " does not exits");
		assertFalse(Files.isRegularFile(console), exe + " should not exits");
		// Check the repository only contains our "modified" launcher
		try (JarFile jarFile = new JarFile(basedir
				.resolve("target/repository/binary/launchericons.executable.win32.win32.x86_64_1.0.0").toFile())) {
			assertNotNull(jarFile.getEntry("eclipse.exe"), "eclipse.exe does not exits in repository");
			assertNull(jarFile.getEntry("eclipsec.exe"), "eclipsec.exe does not exits in repository");
		}
		// Check the cache only contains our "modified" launcher
		try (JarFile jarFile = new JarFile(basedir.resolve(
				"target/products/launchericons/win32/win32/x86_64/p2/org.eclipse.equinox.p2.core/cache/binary/launchericons.executable.win32.win32.x86_64_1.0.0")
				.toFile())) {
			assertNotNull(jarFile.getEntry("eclipse.exe"), "eclipse.exe does not exits in repository");
			assertNull(jarFile.getEntry("eclipsec.exe"), "eclipsec.exe does not exits in repository");
		}
	}

	@Test
	public void testAutomaticManifest() throws Exception {
		Verifier verifier = runDemo("pde-automatic-manifest");
		BundleDescription description = BundlesAction.createBundleDescription(Path
				.of(verifier.getBasedir(), "tycho.demo.service.impl/target/tycho.demo.service.impl-1.0.0-SNAPSHOT.jar")
				.toFile());
		assertNotNull(description, "demo bundle was not packed");
		@SuppressWarnings("unchecked")
		Dictionary<String, String> manifest = (Dictionary<String, String>) description.getUserObject();
		assertEquals("OSGI-INF/tycho.demo.service.impl.InverterServiceImpl.xml", manifest.get("Service-Component"),
				"Service component not found");
		assertTrue(Arrays.stream(description.getImportPackages())
				.anyMatch(pkg -> "tycho.demo.service.api".equals(pkg.getName())), "tycho.demo.service.api package not imported");
	}

	@Test
	public void testTychoJustJDemo() throws Exception {
		assertIncludesJustJ(new File(runDemo("justj", "-f", "product").getBasedir(),
				"product/target/products/product-with-justj-features"));
		assertIncludesJustJ(new File(runDemo("justj", "-f", "automaticInstall").getBasedir(),
				"automaticInstall/target/products/product-with-justj-features"));
	}

	@Test
	public void testSureFireDemo() throws Exception {
		runDemo("testing/surefire/", "-f", "with-maven-layout");
		runDemo("testing/surefire/", "-f", "with-source-folder");
	}

	@Test
	public void testTychoSureFireDemo() throws Exception {
		runDemo("testing/tycho/", "-f", "standalone");
		runDemo("testing/tycho/", "-f", "samemodule");
		runDemo("testing/tycho/", "-f", "osgitest");
	}

	@Test
	@Disabled
	public void testTychoBndDemo() throws Exception {
		runDemo("testing/bnd/", "-f", "osgi-test");
		// TODO add a TCK test demo, e.g. when h2 complies to the jdbc spec we can use
		// that as it is small and fast
	}

	@Test
	public void testTychoJunitPlatformDemo() throws Exception {
		// test with default embedded container...
		runDemo("testing/junit-platform/").verifyTextInLog("1 tests found");
		// test with the forked mode
		runDemo("testing/junit-platform/", "-Djunit-platform.launchType=forked").verifyTextInLog("1 tests found");
		// test that it fails
		try {
			runDemo("testing/junit-platform/", "-Djunit-platform.launchType=unknown");
			fail();
		} catch (VerificationException e) {
			assertTrue(e.getMessage().contains("Launch type 'unknown' is not available"));
		}
	}

	@Test
	public void testTychoBndWorkspaceDemo() throws Exception {
		Verifier verifier = runDemo("bnd-workspace");
		String expectedLocation = "tycho.demo.impl/target/executable/tycho.demo.app.jar";
		File exportedJar = Path.of(verifier.getBasedir(), expectedLocation).toFile();
		assertTrue(exportedJar.exists(), "Did not find exported executable jar at expected location: " + expectedLocation);
	}

	@Test
	public void testTychoMultiReleaseDemo() throws Exception {
		runDemo("multi-release-jar");
	}

	@Test
	public void testTychoMultiReleaseClasspathDemo() throws Exception {
		runDemo("multi-release-jar-classpath");
	}

	@Test
	public void testTychoBndPdeWorkspaceDemo() throws Exception {
		runDemo("bnd-pde-workspace");
	}

	@Test
	public void testTychoPublishP2Demo() throws Exception {
		runDemo("publish-p2");
	}

	@Test
	public void testTychoPromoteP2Demo() throws Exception {
		Verifier verifier = runDemo("promote-p2");
		String expectedLocation = "promotion/target/updatesite/updates/index.html";
		Path indexHtml = Path.of(verifier.getBasedir(), expectedLocation);
		assertTrue(Files.exists(indexHtml),
				"Did not find HTML page for update site at expected location: " + expectedLocation);
	}

	@Test
	public void testP2MavenRepositoryDemo() throws Exception {
		runDemo("p2-maven-site", "deploy", "-DaltDeploymentRepository=snapshot-repo::default::file:maven-repository");
	}

	@Test
	public void testOsgiMavenRepositoryDemo() throws Exception {
		runDemo("osgi-repository", "deploy", "-DaltDeploymentRepository=snapshot-repo::default::file:maven-repository");
	}

	protected Verifier runDemo(String test, String... xargs) throws Exception {
		Verifier verifier = super.getVerifier("../../demo/" + test, true, true);
		for (String xarg : xargs) {
			verifier.addCliOption(xarg);
		}
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
		return verifier;
	}
}
