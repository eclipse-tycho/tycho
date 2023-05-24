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

import java.util.List;

import org.eclipse.pde.core.target.ITargetLocation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DependencyExclusionTest extends AbstractMavenTargetTest {
	@Parameter(0)
	public Boolean includeSource;

	@Parameters(name = "includeSource={0}")
	public static List<Boolean> dependencyConfigurations() {
		return List.of(false, true);
	}

	@Test
	public void testExclusionOfDirectRequirement() throws Exception {
		ITargetLocation target = resolveMavenTarget(String.format(
				"""
						<location includeDependencyDepth="infinite" includeDependencyScopes="compile" includeSource="%s" missingManifest="error" type="Maven">
							<dependencies>
								<dependency>
									<groupId>org.junit.platform</groupId>
									<artifactId>junit-platform-commons</artifactId>
									<version>1.9.3</version>
									<type>jar</type>
								</dependency>
							</dependencies>
							<exclude>org.apiguardian:apiguardian-api:1.1.2</exclude>
						</location>
						""",
				includeSource));
		assertStatusOk(target.getStatus());
		assertArrayEquals(EMPTY, target.getFeatures());
		List<ExpectedBundle> expectedBundles = List.of(junitPlatformCommons("1.9.3"));
		assertTargetBundles(target, includeSource ? withSourceBundles(expectedBundles) : expectedBundles);
	}

	@Test
	public void testExclusionOfDirectAndTransitivRequirement() throws Exception {
		ITargetLocation target = resolveMavenTarget(String
				.format(
				"""
						<location includeDependencyDepth="infinite" includeDependencyScopes="compile" includeSource="%s" missingManifest="error" type="Maven">
							<dependencies>
								<dependency>
									<groupId>org.junit.jupiter</groupId>
									<artifactId>junit-jupiter-api</artifactId>
									<version>5.9.3</version>
									<type>jar</type>
								</dependency>
							</dependencies>
							<exclude>org.apiguardian:apiguardian-api:1.1.2</exclude>
						</location>
						""",
				includeSource));
		assertStatusOk(target.getStatus());
		assertArrayEquals(EMPTY, target.getFeatures());
		List<ExpectedBundle> expectedBundles = List.of(//
				junitJupiterAPI(), //
				junitPlatformCommons("1.9.3"), //
				opentest4j());
		assertTargetBundles(target, includeSource ? withSourceBundles(expectedBundles) : expectedBundles);

	}

	@Test
	public void testExclusionOfMultipleVersions() throws Exception {
		ITargetLocation target = resolveMavenTarget(String
				.format(
				"""
						<location includeDependencyDepth="infinite" includeDependencyScopes="compile" includeSource="%s" missingManifest="error" type="Maven">
							<dependencies>
								<dependency>
									<groupId>org.junit.jupiter</groupId>
									<artifactId>junit-jupiter-api</artifactId>
									<version>5.9.3</version>
									<type>jar</type>
								</dependency>
								<dependency>
									<groupId>org.junit.platform</groupId>
									<artifactId>junit-platform-commons</artifactId>
									<version>1.7.2</version>
									<type>jar</type>
								</dependency>
							</dependencies>
							<exclude>org.apiguardian:apiguardian-api:1.1.2</exclude>
							<exclude>org.apiguardian:apiguardian-api:1.1.0</exclude>
						</location>
						""",
				includeSource));
		assertStatusOk(target.getStatus());
		assertArrayEquals(EMPTY, target.getFeatures());
		List<ExpectedBundle> expectedBundles = List.of(//
				junitJupiterAPI(), //
				junitPlatformCommons("1.9.3"), //
				junitPlatformCommons("1.7.2"), //
				opentest4j());
		assertTargetBundles(target, includeSource ? withSourceBundles(expectedBundles) : expectedBundles);
	}

	@Test
	public void testExclusionOfDifferentVersions() throws Exception {
		ITargetLocation target = resolveMavenTarget(String
				.format(
				"""
						<location includeDependencyDepth="infinite" includeDependencyScopes="compile" includeSource="%s" missingManifest="error" type="Maven">
							<dependencies>
								<dependency>
									<groupId>org.junit.jupiter</groupId>
									<artifactId>junit-jupiter-api</artifactId>
									<version>5.9.3</version>
									<type>jar</type>
								</dependency>
								<dependency>
									<groupId>org.junit.platform</groupId>
									<artifactId>junit-platform-commons</artifactId>
									<version>1.7.2</version>
									<type>jar</type>
								</dependency>
							</dependencies>
							<exclude>org.apiguardian:apiguardian-api:1.1.0</exclude>
						</location>
						""",
				includeSource));
		assertStatusOk(target.getStatus());
		assertArrayEquals(EMPTY, target.getFeatures());
		List<ExpectedBundle> expectedBundles = List.of(//
				junitJupiterAPI(), //
				junitPlatformCommons("1.9.3"), //
				junitPlatformCommons("1.7.2"), //
				apiGuardian("1.1.2"), opentest4j());
		assertTargetBundles(target, includeSource ? withSourceBundles(expectedBundles) : expectedBundles);
	}

	private static ExpectedBundle junitPlatformCommons(String version) {
		return originalOSGiBundle("junit-platform-commons", version, "org.junit.platform:junit-platform-commons");
	}

	private static ExpectedBundle junitJupiterAPI() {
		return originalOSGiBundle("junit-jupiter-api", "5.9.3", "org.junit.jupiter:junit-jupiter-api");
	}

	private static ExpectedBundle apiGuardian(String version) {
		return originalOSGiBundle("org.apiguardian.api", version, "org.apiguardian:apiguardian-api");
	}

	private static ExpectedBundle opentest4j() {
		return originalOSGiBundle("org.opentest4j", "1.2.0", "org.opentest4j:opentest4j");
	}
}
