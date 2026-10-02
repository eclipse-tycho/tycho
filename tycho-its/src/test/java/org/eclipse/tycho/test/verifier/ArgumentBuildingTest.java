/*******************************************************************************
 * Copyright (c) 2026 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.verifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers the argument-building logic that replaced maven-verifier 1.8.0's
 * {@code Verifier#executeGoals}/{@code ForkedLauncher#run}: none of these tests launch Maven, they
 * only assert on the CLI argument list {@link Verifier#buildArguments(List)} would hand to
 * {@code ForkedMavenExecutor}.
 */
class ArgumentBuildingTest {

	private String previousMavenHome;

	private String previousMavenRepoLocal;

	@TempDir
	Path basedir;

	@TempDir
	Path localRepo;

	@BeforeEach
	void stubMavenHome() {
		previousMavenHome = System.getProperty("maven.home");
		previousMavenRepoLocal = System.getProperty("maven.repo.local");
		// the constructor only needs *a* value here, ForkedMavenExecutor is never invoked by these tests
		System.setProperty("maven.home", basedir.toString());
		System.setProperty("maven.repo.local", localRepo.toString());
	}

	@AfterEach
	void restoreSystemProperties() {
		restore("maven.home", previousMavenHome);
		restore("maven.repo.local", previousMavenRepoLocal);
	}

	private static void restore(String key, String previousValue) {
		if (previousValue == null) {
			System.clearProperty(key);
		} else {
			System.setProperty(key, previousValue);
		}
	}

	private Verifier newVerifier() throws VerificationException {
		return new Verifier(basedir.toString());
	}

	@Test
	void addCliOptionWithEmbeddedSpaceIsSplitIntoSeparateArguments() throws VerificationException {
		// AbstractTychoIntegrationTest builds exactly this shape: addCliOption("-s " + settingsPath).
		// It must come out as two separate process arguments, not one "-s /path" argv element,
		// or the forked mvn sees an unknown single argument instead of the -s option and its value.
		Path settings = basedir.resolve("settings.xml");
		Verifier verifier = newVerifier();
		verifier.addCliOption("-s " + settings);

		List<String> args = verifier.buildArguments(List.of("verify"));

		int index = args.indexOf("-s");
		assertTrue(index >= 0, () -> "-s not found in " + args);
		assertEquals(settings.toString(), args.get(index + 1));
	}

	@Test
	void basedirPlaceholderIsSubstitutedBeforeTokenizing() throws VerificationException {
		Verifier verifier = newVerifier();
		verifier.addCliOption("-Dtest.dir=${basedir}/data");

		List<String> args = verifier.buildArguments(List.of("verify"));

		assertTrue(args.contains("-Dtest.dir=" + basedir + "/data"), args::toString);
	}

	@Test
	void doubledSlashesCollapseToPreserveUrlScheme() throws VerificationException {
		// several Tycho ITs write "https:////host/path" specifically to survive this single
		// collapsing pass with the intended "https://host/path" URL intact.
		Verifier verifier = newVerifier();
		verifier.addCliOption("-Drepo=https:////download.eclipse.org/releases/2024-09/");

		List<String> args = verifier.buildArguments(List.of("verify"));

		assertTrue(args.contains("-Drepo=https://download.eclipse.org/releases/2024-09/"), args::toString);
	}

	@Test
	void setSystemPropertyBecomesADArgument() throws VerificationException {
		Verifier verifier = newVerifier();
		verifier.setSystemProperty("allowMajorUpdates", "false");

		List<String> args = verifier.buildArguments(List.of("verify"));

		assertTrue(args.contains("-DallowMajorUpdates=false"), args::toString);
	}

	@Test
	void autocleanPrependsTheCleanPluginGoal() throws VerificationException {
		Verifier verifier = newVerifier();

		List<String> args = verifier.buildArguments(List.of("verify"));

		int cleanIndex = args.indexOf("org.apache.maven.plugins:maven-clean-plugin:clean");
		int verifyIndex = args.indexOf("verify");
		assertTrue(cleanIndex >= 0 && cleanIndex < verifyIndex, args::toString);
	}

	@Test
	void autocleanCanBeDisabled() throws VerificationException {
		Verifier verifier = newVerifier();
		verifier.setAutoclean(false);

		List<String> args = verifier.buildArguments(List.of("verify"));

		assertFalse(args.contains("org.apache.maven.plugins:maven-clean-plugin:clean"), args::toString);
	}

	@Test
	void defaultOptionsAreAlwaysPresent() throws VerificationException {
		Verifier verifier = newVerifier();

		List<String> args = verifier.buildArguments(List.of("verify"));

		assertTrue(args.contains("-e"), args::toString);
		assertTrue(args.contains("--batch-mode"), args::toString);
	}

	@Test
	void mavenRepoLocalIsPinnedByDefault() throws VerificationException {
		Verifier verifier = newVerifier();

		List<String> args = verifier.buildArguments(List.of("verify"));

		assertTrue(args.contains("-Dmaven.repo.local=" + verifier.getLocalRepository()), args::toString);
	}

	@Test
	void mavenRepoLocalCanBeOptedOut() throws VerificationException {
		Verifier verifier = newVerifier();
		verifier.getVerifierProperties().setProperty("use.mavenRepoLocal", "false");

		List<String> args = verifier.buildArguments(List.of("verify"));

		assertFalse(args.stream().anyMatch(a -> a.startsWith("-Dmaven.repo.local=")), args::toString);
	}

	@Test
	void translateCommandlineSplitsOnUnquotedSpaces() throws VerificationException {
		String[] tokens = Verifier.translateCommandline("-s /tmp/x settings");

		assertEquals(List.of("-s", "/tmp/x", "settings"), List.of(tokens));
	}

	@Test
	void translateCommandlineKeepsQuotedSpacesInOneToken() throws VerificationException {
		String[] tokens = Verifier.translateCommandline("-Dtitle=\"hello world\" -Dnext=value");

		assertEquals(List.of("-Dtitle=hello world", "-Dnext=value"), List.of(tokens));
	}

	@Test
	void translateCommandlineRejectsUnbalancedQuotes() {
		assertThrows(VerificationException.class, () -> Verifier.translateCommandline("-Dfoo='bar"));
	}

}
