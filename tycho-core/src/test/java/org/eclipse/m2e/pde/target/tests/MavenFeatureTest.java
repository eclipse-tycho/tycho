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

import java.util.List;

import org.eclipse.pde.core.target.ITargetLocation;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class MavenFeatureTest extends AbstractMavenTargetTest {
	@Parameter(0)
	public Boolean includeSource;

	@Parameters(name = "includeSource={0}")
	public static List<Boolean> dependencyConfigurations() {
		return List.of(false, true);
	}

	@Test
	public void testLocationContentFeatureGeneration() throws Exception {
		ITargetLocation target = resolveMavenTarget(String
				.format(
				"""
						<location includeDependencyDepth="infinite" includeDependencyScopes="compile" includeSource="%s" missingManifest="error" type="Maven">
							<feature id="my.junit.feature" label="My Junit Feature" provider-name="Me Inc." version="1.2.3.qualifier">
								<description url="http://www.example.com/description">
									This is a great feature
								</description>
								<copyright url="http://www.example.com/copyright">
									My copyright description.
								</copyright>
								<license url="http://www.example.com/license">
									No license granted.
								</license>
								<plugin download-size="0" id="slf4j.api" install-size="0" unpack="false" version="0.0.0"/>
							</feature>
							<dependencies>
								<dependency>
									<groupId>org.junit.jupiter</groupId>
									<artifactId>junit-jupiter-api</artifactId>
									<version>5.9.3</version>
									<type>jar</type>
								</dependency>
							</dependencies>
						</location>
						""",
				includeSource));
		assertStatusOk(target.getStatus());
		List<ExpectedBundle> expectedBundles = List.of( //
				junitJupiterAPI(), //
				junitPlatformCommons(), //
				apiGuardian(), opentest4j());
		assertTargetBundles(target, includeSource ? withSourceBundles(expectedBundles) : expectedBundles);
		List<ExpectedFeature> expectedFeature = List.of(generatedFeature("my.junit.feature", "1.2.3.qualifier", List.of(//
				featurePlugin("junit-jupiter-api", "5.9.3"), //
				featurePlugin("junit-platform-commons", "1.9.3"), //
				featurePlugin("org.apiguardian.api", "1.1.2"), //
				featurePlugin("org.opentest4j", "1.2.0"), //
				featurePlugin("slf4j.api", null))));
		List<ExpectedFeature> expectedFeatures = includeSource ? withSourceFeatures(expectedFeature) : expectedFeature;
		expectedFeatures = expectedFeatures.stream().map(f -> {
			if (f.containedPlugins().stream().noneMatch(d -> d.getId().equals("slf4j.api.source"))) {
				return f;
			}
			// Explicitly listed Plug-ins are just removed for a Source-Feature, if they are
			// (probably) not a source Plug-in and are not mapped to a source Plug-in.
			return new ExpectedFeature(f.id(), f.version(), f.isSourceBundle(), f.isOriginal(), f.key(),
					f.containedPlugins().stream().filter(d -> !d.getId().equals("slf4j.api.source")).toList());
		}).toList();
		assertTargetFeatures(target, expectedFeatures);
	}

	@Test
	public void testPomArtifactFeatureGeneration() throws Exception {
		ITargetLocation target = resolveMavenTarget(String
				.format(
				"""
						<location includeDependencyDepth="infinite" includeDependencyScopes="compile" includeSource="%s" missingManifest="error" type="Maven">
							<dependencies>
								<dependency>
									<groupId>com.sun.xml.bind</groupId>
									<artifactId>jaxb-ri</artifactId>
									<version>4.0.2</version>
									<type>pom</type>
								</dependency>
							</dependencies>
						</location>
						""",
				includeSource));
		assertStatusOk(target.getStatus());
		List<ExpectedBundle> expectedBundles = List.of( //
				originalOSGiBundle("com.sun.xml.bind.jaxb-core", "4.0.2", "com.sun.xml.bind:jaxb-core"),
				originalOSGiBundle("com.sun.xml.bind.jaxb-impl", "4.0.2", "com.sun.xml.bind:jaxb-impl"),
				originalOSGiBundle("com.sun.xml.bind.jaxb-jxc", "4.0.2", "com.sun.xml.bind:jaxb-jxc"),
				originalOSGiBundle("com.sun.xml.bind.jaxb-xjc", "4.0.2", "com.sun.xml.bind:jaxb-xjc"),
				originalOSGiBundle("com.sun.xml.fastinfoset.FastInfoset", "2.1.0",
						"com.sun.xml.fastinfoset:FastInfoset"),
				originalOSGiBundle("jakarta.activation-api", "2.1.1", "jakarta.activation:jakarta.activation-api"),
				originalOSGiBundle("jakarta.xml.bind-api", "4.0.0", "jakarta.xml.bind:jakarta.xml.bind-api"),
				originalOSGiBundle("org.jvnet.staxex.stax-ex", "2.1.0", "org.jvnet.staxex:stax-ex"));
		assertTargetBundles(target, includeSource ? withSourceBundles(expectedBundles) : expectedBundles);
		List<ExpectedFeature> expectedFeature = List.of(generatedFeature("com.sun.xml.bind.jaxb-ri.pom", "0.0.1",
				List.of(//
						featurePlugin("com.sun.xml.bind.jaxb-core", "4.0.2"),
						featurePlugin("com.sun.xml.bind.jaxb-impl", "4.0.2"),
						featurePlugin("com.sun.xml.bind.jaxb-jxc", "4.0.2"),
						featurePlugin("com.sun.xml.bind.jaxb-xjc", "4.0.2"),
						featurePlugin("com.sun.xml.fastinfoset.FastInfoset", "2.1.0"),
						featurePlugin("jakarta.activation-api", "2.1.1"),
						featurePlugin("jakarta.xml.bind-api", "4.0.0"),
						featurePlugin("org.jvnet.staxex.stax-ex", "2.1.0"))));
		assertTargetFeatures(target, includeSource ? withSourceFeatures(expectedFeature) : expectedFeature);

	}

	@Test
	public void testFeatureArtifact() throws Exception {
		// TODO: For real feature artifacts, which don't have a source-artifact, a
		// source feature is not generated (yet).
		Assume.assumeFalse(includeSource);
		ITargetLocation target = resolveMavenTarget(String
				.format(
				"""
						<location includeDependencyDepth="infinite" includeDependencyScopes="compile" includeSource="%s" missingManifest="error" type="Maven">
							<dependencies>
								<dependency>
									<groupId>org.eclipse.vorto</groupId>
									<artifactId>org.eclipse.vorto.feature</artifactId>
									<version>1.0.0</version>
									<type>jar</type>
								</dependency>
							</dependencies>
						</location>
						""",
				includeSource));
		assertStatusOk(target.getStatus());
		assertTargetBundles(target, List.of());
		List<ExpectedFeature> expectedFeature = List.of(generatedFeature("org.eclipse.vorto.feature", "1.0.0", List.of(//
				featurePlugin("org.eclipse.vorto.core", "1.0.0"), //
				featurePlugin("org.eclipse.vorto.editor", "1.0.0"), //
				featurePlugin("org.eclipse.vorto.editor.datatype", "1.0.0"),
				featurePlugin("org.eclipse.vorto.editor.datatype.ide", "1.0.0"),
				featurePlugin("org.eclipse.vorto.editor.datatype.ui", "1.0.0"),
				featurePlugin("org.eclipse.vorto.editor.functionblock", "1.0.0"),
				featurePlugin("org.eclipse.vorto.editor.functionblock.ide", "1.0.0"),
				featurePlugin("org.eclipse.vorto.editor.functionblock.ui", "1.0.0"),
				featurePlugin("org.eclipse.vorto.editor.infomodel", "1.0.0"),
				featurePlugin("org.eclipse.vorto.editor.infomodel.ide", "1.0.0"),
				featurePlugin("org.eclipse.vorto.editor.infomodel.ui", "1.0.0"),
				featurePlugin("org.eclipse.vorto.editor.mapping", "1.0.0"),
				featurePlugin("org.eclipse.vorto.editor.mapping.ide", "1.0.0"),
				featurePlugin("org.eclipse.vorto.editor.mapping.ui", "1.0.0"))));
		assertTargetFeatures(target, includeSource ? withSourceFeatures(expectedFeature) : expectedFeature);
	}

	private static ExpectedBundle junitPlatformCommons() {
		return originalOSGiBundle("junit-platform-commons", "1.9.3", "org.junit.platform:junit-platform-commons");
	}

	private static ExpectedBundle junitJupiterAPI() {
		return originalOSGiBundle("junit-jupiter-api", "5.9.3", "org.junit.jupiter:junit-jupiter-api");
	}

	private static ExpectedBundle apiGuardian() {
		return originalOSGiBundle("org.apiguardian.api", "1.1.2", "org.apiguardian:apiguardian-api");
	}

	private static ExpectedBundle opentest4j() {
		return originalOSGiBundle("org.opentest4j", "1.2.0", "org.opentest4j:opentest4j");
	}
}
