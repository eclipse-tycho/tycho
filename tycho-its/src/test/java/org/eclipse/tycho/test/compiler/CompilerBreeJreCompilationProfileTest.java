/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.compiler;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.nio.file.Path;

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * When a bundle has no <code>Bundle-RequiredExecutionEnvironment</code>
 * manifest header, the compiler's <code>useJDK=BREE</code> toolchain lookup
 * must prefer the effective compiler level, i.e. build.properties'
 * <code>jre.compilation.profile</code> (the same setting that already
 * determines the effective -source/-target level, see
 * AbstractOsgiCompilerMojo#getSourceLevel()/getTargetLevel()), over the
 * reactor-wide target-platform-configuration executionEnvironment, which is
 * unrelated to this bundle's own compiler compliance level.
 * <p>
 * The test project provides two distinguishable fake toolchains, one for
 * JavaSE-1.8 (which build.properties' jre.compilation.profile declares) and one
 * for JavaSE-17 (the unrelated target-platform-configuration
 * executionEnvironment). The compiler mojo logs a warning mentioning the
 * toolchain's JDK home it actually picked while scanning for a boot classpath,
 * so we can assert which of the two toolchains was used without needing a real
 * second JDK.
 */
public class CompilerBreeJreCompilationProfileTest extends AbstractTychoIntegrationTest {

	@Test
	public void testJreCompilationProfileTakesPrecedenceOverTargetPlatformEE() throws Exception {
		Verifier verifier = getVerifier("compiler.bree.jreCompilationProfile", false);
		File toolchains = new File(verifier.getBasedir(), "toolchains.xml");
		verifier.addCliOption("--toolchains " + toolchains.getCanonicalPath());
		verifier.executeGoal("compile");
		verifier.verifyErrorFreeLog();
		// The JavaSE-1.8 toolchain (matching build.properties' jre.compilation.profile)
		// must be picked, not the JavaSE-17 toolchain (matching the unrelated
		// target-platform-configuration executionEnvironment).
		verifier.verifyTextInLog(jdkPath(18) + " not found");
		String log = String.join("\n", verifier.loadFile(verifier.getBasedir(), verifier.getLogFileName(), false));
		assertFalse(log.contains(jdkPath(17) + " not found"),
				"JavaSE-17 toolchain (target-platform-configuration executionEnvironment) was used instead of JavaSE-1.8 (jre.compilation.profile)");
	}

	private Path jdkPath(int version) {
		return Path.of("fake-jdk-home-" + version, "bin", SystemUtils.IS_OS_WINDOWS ? "java.exe" : "java");
	}
}
