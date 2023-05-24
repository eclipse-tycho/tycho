/*******************************************************************************
 * Copyright (c) 2023, 2023 Hannes Wellmann and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.target.tests;

import static org.junit.Assert.assertArrayEquals;

import java.util.Collection;
import java.util.List;

import org.eclipse.pde.core.target.ITargetLocation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class MavenTargetsTransitiveDependenciesTest extends AbstractMavenTargetTest {

	@Parameter(0)
	public String dependencyDepth;
	@Parameter(1)
	public String dependencyScopes;
	@Parameter(2)
	public String sources;
	@Parameter(3)
	public List<ExpectedBundle> expectedBundles;

	@Parameters(name = "includeDependencyDepth={0} - includeDependencyScopes={1} - includeSource={2}")
	public static Collection<Object[]> dependencyConfigurations() {
		return List.of(//
				new Object[] { "none", "", "false", List.of( //
						junitJupiter("junit-jupiter")) },

				new Object[] { "none", "", "true", withSourceBundles(List.of( //
						junitJupiter("junit-jupiter"))) },

				new Object[] { "direct", "compile", "false", List.of( //
						junitJupiter("junit-jupiter"), //
						junitJupiter("junit-jupiter-api"), //
						junitJupiter("junit-jupiter-params")) },

				new Object[] { "direct", "compile", "true", withSourceBundles(List.of( //
						junitJupiter("junit-jupiter"), //
						junitJupiter("junit-jupiter-api"), //
						junitJupiter("junit-jupiter-params"))) },

				new Object[] { "direct", "compile,runtime", "false", List.of( //
						junitJupiter("junit-jupiter"), //
						junitJupiter("junit-jupiter-api"), //
						junitJupiter("junit-jupiter-params"), //
						junitJupiter("junit-jupiter-engine")) },

				new Object[] { "direct", "compile,runtime", "true", withSourceBundles(List.of( //
						junitJupiter("junit-jupiter"), //
						junitJupiter("junit-jupiter-api"), //
						junitJupiter("junit-jupiter-params"), //
						junitJupiter("junit-jupiter-engine"))) },

				new Object[] { "infinite", "compile", "false", List.of( //
						junitJupiter("junit-jupiter"), //
						junitJupiter("junit-jupiter-api"), //
						junitJupiter("junit-jupiter-params"), //
						junitPlatform("junit-platform-commons"), //
						apiGuardian(), opentest4j()) },

				new Object[] { "infinite", "compile", "true", withSourceBundles(List.of(//
						junitJupiter("junit-jupiter"), //
						junitJupiter("junit-jupiter-api"), //
						junitJupiter("junit-jupiter-params"), //
						junitPlatform("junit-platform-commons"), //
						apiGuardian(), opentest4j())) },

				new Object[] { "infinite", "compile,runtime", "false", List.of( //
						junitJupiter("junit-jupiter"), //
						junitJupiter("junit-jupiter-api"), //
						junitJupiter("junit-jupiter-params"), //
						junitJupiter("junit-jupiter-engine"), //
						junitPlatform("junit-platform-commons"), //
						junitPlatform("junit-platform-engine"), //
						apiGuardian(), opentest4j()) },

				new Object[] { "infinite", "compile,runtime", "true", withSourceBundles(List.of( //
						junitJupiter("junit-jupiter"), //
						junitJupiter("junit-jupiter-api"), //
						junitJupiter("junit-jupiter-params"), //
						junitJupiter("junit-jupiter-engine"), //
						junitPlatform("junit-platform-commons"), //
						junitPlatform("junit-platform-engine"), //
						apiGuardian(), opentest4j())) }

		);
	}

	private static ExpectedBundle junitJupiter(String artifactID) {
		return originalOSGiBundle(artifactID, "5.9.3", "org.junit.jupiter:" + artifactID);
	}

	private static ExpectedBundle junitPlatform(String artifactID) {
		return originalOSGiBundle(artifactID, "1.9.3", "org.junit.platform:" + artifactID);
	}

	private static ExpectedBundle apiGuardian() {
		return originalOSGiBundle("org.apiguardian.api", "1.1.2", "org.apiguardian:apiguardian-api");
	}

	private static ExpectedBundle opentest4j() {
		return originalOSGiBundle("org.opentest4j", "1.2.0", "org.opentest4j:opentest4j");
	}

	@Test
	public void testSingleRootArtifact() throws Exception {
		String targetDefinition = """
				<location includeDependencyDepth="%s" includeDependencyScopes="%s" includeSource="%s" missingManifest="error" type="Maven">
					<dependencies>
						<dependency>
							<groupId>org.junit.jupiter</groupId>
							<artifactId>junit-jupiter</artifactId>
							<version>5.9.3</version>
							<type>jar</type>
						</dependency>
					</dependencies>
				</location>
				"""
				.formatted(dependencyDepth, dependencyScopes, sources);
		ITargetLocation target = resolveMavenTarget(targetDefinition);
		assertTargetBundles(target, expectedBundles);
		assertArrayEquals(EMPTY, target.getFeatures());
	}
}