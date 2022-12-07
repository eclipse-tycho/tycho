/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.feature;

import java.io.File;
import java.util.List;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class FeatureWithDependenciesTest extends AbstractTychoIntegrationTest {

	@Test
	public void testFeatureRestrictionWithIncludePluginsEnabled() throws Exception {
		Verifier verifier = getVerifier("feature.dependency", false, true);
		verifier.setSystemProperty("includePlugins", "true");
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File targetdir = new File(verifier.getBasedir(), "site/target");
		assertFileExists(targetdir, "repository/plugins/org.aopalliance_*.jar");
		assertFileExists(targetdir, "repository/features/test.include.feature_*.jar");
		assertFileExists(targetdir, "repository/plugins/com.google.guava_*.jar");
		assertFileDoesNotExist(targetdir, "repository/plugins/org.eclipse.emf.common_2*.jar");
		assertFileDoesNotExist(targetdir, "repository/features/org.eclipse.emf.sdk_*.jar");
	}

	@Test
	public void testFeatureRestrictionWithIncludeFeaturesEnabled() throws Exception {
		Verifier verifier = getVerifier("feature.dependency", false, true);
		verifier.setSystemProperty("includeFeatures", "true");
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File targetdir = new File(verifier.getBasedir(), "site/target");
		assertFileExists(targetdir, "repository/plugins/org.aopalliance_*.jar");
		assertFileExists(targetdir, "repository/features/test.include.feature_*.jar");
		assertFileDoesNotExist(targetdir, "repository/plugins/com.google.guava_*.jar");
		assertFileExists(targetdir, "repository/plugins/org.eclipse.emf.common_2*.jar");
		assertFileExists(targetdir, "repository/features/org.eclipse.emf.sdk_*.jar");
	}

	@Test
	public void testFeatureRestrictionWithIncludePluginsAndIncludeFeaturesEnabled() throws Exception {
		Verifier verifier = getVerifier("feature.dependency", false, true);
		verifier.setSystemProperty("includePlugins", "true");
		verifier.setSystemProperty("includeFeatures", "true");
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File targetdir = new File(verifier.getBasedir(), "site/target");
		assertFileExists(targetdir, "repository/plugins/org.aopalliance_*.jar");
		assertFileExists(targetdir, "repository/features/test.include.feature_*.jar");
		assertFileExists(targetdir, "repository/plugins/com.google.guava_*.jar");
		assertFileExists(targetdir, "repository/plugins/org.eclipse.emf.common_2*.jar");
		assertFileExists(targetdir, "repository/features/org.eclipse.emf.sdk_*.jar");
	}

	@Test
	public void testFeatureRestrictionWithIncludePluginsAndFeaturesDisabled() throws Exception {
		Verifier verifier = getVerifier("feature.dependency", false, true);
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File targetdir = new File(verifier.getBasedir(), "site/target");
		assertFileExists(targetdir, "repository/plugins/org.aopalliance_*.jar");
		assertFileExists(targetdir, "repository/features/test.include.feature_*.jar");
		assertFileDoesNotExist(targetdir, "repository/plugins/com.google.guava_*.jar");
		assertFileDoesNotExist(targetdir, "repository/plugins/org.eclipse.emf.common_2.*.jar");
		assertFileDoesNotExist(targetdir, "repository/features/org.eclipse.emf.sdk_*.jar");
	}
}
