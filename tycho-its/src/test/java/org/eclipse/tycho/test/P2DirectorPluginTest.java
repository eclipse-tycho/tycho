package org.eclipse.tycho.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.TargetEnvironment;
import org.junit.Test;

public class P2DirectorPluginTest extends AbstractTychoIntegrationTest {

	@Test
	public void testDirectorStandaloneWindows() throws Exception {
		Verifier verifier = getVerifier("tycho-p2-director-plugin/director-goal-standalone", true, true);
		verifier.addCliArgument("-Pdirector-windows");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		assertTrue(Files.isDirectory(Path.of(verifier.getBasedir(), "target", "productwindows", "plugins")));
	}

	@Test
	public void testDirectorStandaloneLinux() throws Exception {
		Verifier verifier = getVerifier("tycho-p2-director-plugin/director-goal-standalone", true, true);
		verifier.addCliArgument("-Pdirector-linux");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		assertTrue(Files.isDirectory(Path.of(verifier.getBasedir(), "target", "productlinux", "plugins")));
	}

	@Test
	public void testDirectorStandaloneMacOsDestinationWithAppSuffix() throws Exception {
		Verifier verifier = getVerifier("tycho-p2-director-plugin/director-goal-standalone", true, true);
		verifier.addCliArgument("-Pdirector-macos-destination-with-app-suffix");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		assertTrue(Files.isDirectory(
				Path.of(verifier.getBasedir(), "target", "productmacos.app", "Contents", "Eclipse", "plugins")));
	}

	@Test
	public void testDirectorStandaloneMacOsDestinationWithoutAppSuffix() throws Exception {
		Verifier verifier = getVerifier("tycho-p2-director-plugin/director-goal-standalone", true, true);
		verifier.addCliArgument("-Pdirector-macos-destination-without-app-suffix");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		assertTrue(Files.isDirectory(Path.of(verifier.getBasedir(), "target", "productmacos", "Eclipse.app", "Contents",
				"Eclipse", "plugins")));
	}

	@Test
	public void testDirectorStandaloneMacOsDestinationWithFullBundlePath() throws Exception {
		Verifier verifier = getVerifier("tycho-p2-director-plugin/director-goal-standalone", true, true);
		verifier.addCliArgument("-Pdirector-macos-destination-with-full-bundle-path");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		assertTrue(Files.isDirectory(
				Path.of(verifier.getBasedir(), "target", "productmacos.app", "Contents", "Eclipse", "plugins")));
	}

	@Test
	public void testDirectorStandaloneUsingRunningEnvironment() throws Exception {
		Verifier verifier = getVerifier("tycho-p2-director-plugin/director-goal-standalone", true, true);
		verifier.addCliArgument("-Pdirector-running-environment");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		if ("macosx".equals(TargetEnvironment.getRunningEnvironment().getOs())) {
			assertTrue(Files.isDirectory(Path.of(verifier.getBasedir(), "target", "product", "Eclipse.app", "Contents",
					"Eclipse", "plugins")));
		} else {
			assertTrue(Files.isDirectory(Path.of(verifier.getBasedir(), "target", "product", "plugins")));
		}
	}

	@Test
	public void testDirectorStandaloneInconsistentP2Options() throws Exception {
		Verifier verifier = getVerifier("tycho-p2-director-plugin/director-goal-standalone", true, true);
		verifier.addCliArgument("-Pdirector-iconsistent-p2-arguments");
		try {
			verifier.executeGoal("package");
			fail(VerificationException.class.getName() + " expected");
		} catch (VerificationException e) {
		}
		verifier.verifyTextInLog(
				"p2os / p2ws / p2arch must be mutually specified, p2os=win32 given, p2ws missing, p2arch missing");
		assertFalse(Files.isDirectory(Path.of(verifier.getBasedir(), "target", "product")));
	}

	@Test
	public void testP2CacheIsDeleted() throws Exception {
		Verifier verifier = getVerifier("product.deleteP2Cache", false);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		File basedir = new File(verifier.getBasedir());
		Path productDir = basedir.toPath().resolve("target/products/test.product/linux/gtk/x86_64");

		// Verify product was created
		assertTrue("Product directory should exist", Files.exists(productDir));

		// Verify p2 cache directory was deleted
		Path cacheDir = productDir.resolve("p2/org.eclipse.equinox.p2.core/cache");
		assertFalse("P2 cache directory should not exist when deleteP2Cache=true", Files.exists(cacheDir));

		// Verify that p2 directory itself still exists (just not the cache
		// subdirectory)
		Path p2Dir = productDir.resolve("p2");
		assertTrue("P2 directory should still exist", Files.exists(p2Dir));
	}

	@Test
	public void testP2CacheIsKeptByDefault() throws Exception {
		Verifier verifier = getVerifier("product.keepP2Cache", false);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		File basedir = new File(verifier.getBasedir());
		Path productDir = basedir.toPath().resolve("target/products/test.product/linux/gtk/x86_64");

		// Verify product was created
		assertTrue("Product directory should exist", Files.exists(productDir));

		// Verify p2 cache directory exists (default behavior - cache should be kept)
		Path cacheDir = productDir.resolve("p2/org.eclipse.equinox.p2.core/cache");
		assertTrue("P2 cache directory should exist by default (deleteP2Cache not set)", Files.exists(cacheDir));
	}

}
