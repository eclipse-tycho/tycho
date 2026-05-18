/*******************************************************************************
 * Copyright (c) 2026 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.target.tests;

import java.util.List;
import java.util.stream.Stream;

import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests that the m2e PDE target correctly handles Maven BOM (Bill of Materials)
 * artifacts, including resolution of coordinates inherited from parent POMs.
 */
@RunWith(Parameterized.class)
public class MavenBomSupportTest extends AbstractMavenTargetTest {

	@Parameter(0)
	public Boolean includeSource;

	@Parameters(name = "includeSource={0}")
	public static List<Boolean> configurations() {
		return List.of(false, true);
	}

	@Test
	public void testBomInheritedGroupIdAndVersionResolvedCorrectly() throws Exception {
		// cucumber-bom:7.34.3 inherits both groupId (io.cucumber) and version (7.34.3)
		// from its parent POM - the raw model returns null for both fields.
		ITargetLocation target = resolveMavenTarget(String.format(
				"""
						<location includeDependencyDepth="none" includeDependencyScopes="compile" includeSource="%s" missingManifest="generate" type="Maven">
							<dependencies>
								<dependency>
									<groupId>io.cucumber</groupId>
									<artifactId>cucumber-bom</artifactId>
									<version>7.34.3</version>
									<type>pom</type>
								</dependency>
							</dependencies>
						</location>
						""",
				includeSource));
		assertStatusOk(target.getStatus());

		// A BOM has no regular <dependencies>, only <dependencyManagement>, so no
		// bundles are contributed.
		assertTargetBundles(target, List.of());

		// The generated feature must use the resolved coordinates, not the raw model
		// values which are null when inherited from the parent POM.
		List<ExpectedFeature> expectedFeatures = List.of(
				generatedFeature("io.cucumber.cucumber-bom.pom", "7.34.3", List.of()));
		assertTargetFeatures(target, includeSource ? withSourceFeatures(expectedFeatures) : expectedFeatures);
	}

	@Test
	public void testBomManagedDependenciesExpanded() throws Exception {
		// With includeDependencyScopes="import", the <dependencyManagement> entries of
		// the BOM should be expanded as direct bundles. Property-based versions (e.g.
		// ${gherkin.version}) must be resolved to their concrete values.
		ITargetLocation target = resolveMavenTarget("""
				<location includeDependencyDepth="infinite" includeDependencyScopes="compile,import" \
				includeSource="false" missingManifest="generate" type="Maven">
					<dependencies>
						<dependency>
							<groupId>io.cucumber</groupId>
							<artifactId>cucumber-bom</artifactId>
							<version>7.34.3</version>
							<type>pom</type>
						</dependency>
					</dependencies>
				</location>
				""");
		assertStatusOk(getTargetStatus(target));

		List<ExpectedBundle> expectedBundles = Stream.concat(bomDirectManagedDependencyBundles().stream(),
				bomTransitiveDependencyBundles().stream()).toList();
		assertTargetBundles(target, expectedBundles);

		List<NameVersionDescriptor> expectedPlugins = expectedBundles.stream()
				.map(b -> featurePlugin(b.bsn(), b.version())).toList();
		assertTargetFeatures(target,
				List.of(generatedFeature("io.cucumber.cucumber-bom.pom", "7.34.3", expectedPlugins)));
	}

	@Test
	public void testBomManagedDependenciesExpandedDirect() throws Exception {
		// With includeDependencyDepth="direct", only the direct <dependencyManagement>
		// entries of the BOM are included — no transitive dependencies of those entries.
		ITargetLocation target = resolveMavenTarget("""
				<location includeDependencyDepth="direct" includeDependencyScopes="compile,import" \
				includeSource="false" missingManifest="generate" type="Maven">
					<dependencies>
						<dependency>
							<groupId>io.cucumber</groupId>
							<artifactId>cucumber-bom</artifactId>
							<version>7.34.3</version>
							<type>pom</type>
						</dependency>
					</dependencies>
				</location>
				""");
		assertStatusOk(getTargetStatus(target));

		List<ExpectedBundle> expectedBundles = bomDirectManagedDependencyBundles();
		assertTargetBundles(target, expectedBundles);

		List<NameVersionDescriptor> expectedPlugins = expectedBundles.stream()
				.map(b -> featurePlugin(b.bsn(), b.version())).toList();
		assertTargetFeatures(target,
				List.of(generatedFeature("io.cucumber.cucumber-bom.pom", "7.34.3", expectedPlugins)));
	}

	private static List<ExpectedBundle> bomDirectManagedDependencyBundles() {
		return List.of(
				// Bundles with original OSGi metadata (property-resolved versions):
				originalOSGiBundle("io.cucumber.gherkin", "36.1.0", "io.cucumber:gherkin"),
				originalOSGiBundle("io.cucumber.cucumberexpressions", "18.1.0", "io.cucumber:cucumber-expressions"),
				originalOSGiBundle("io.cucumber.messages", "30.1.0", "io.cucumber:messages"),
				originalOSGiBundle("io.cucumber.messagesn.ndjson", "0.3.2", "io.cucumber:messages-ndjson"),
				// Wrapped bundles (property-resolved versions):
				cucumberWrapped("ci-environment", "12.0.0"),
				cucumberWrapped("cucumber-json-formatter", "0.3.2"),
				cucumberWrapped("html-formatter", "22.3.0"),
				cucumberWrapped("junit-xml-formatter", "0.11.0"),
				cucumberWrapped("pretty-formatter", "2.4.1"),
				cucumberWrapped("query", "14.7.0"),
				cucumberWrapped("tag-expressions", "8.1.0"),
				cucumberWrapped("teamcity-formatter", "0.2.0"),
				cucumberWrapped("testng-xml-formatter", "0.7.0"),
				cucumberWrapped("usage-formatter", "0.1.0"),
				// Wrapped bundles (direct version 7.34.3):
				cucumberWrapped("cucumber-cdi2", "7.34.3"),
				cucumberWrapped("cucumber-core", "7.34.3"),
				cucumberWrapped("datatable", "7.34.3"),
				cucumberWrapped("datatable-matchers", "7.34.3"),
				cucumberWrapped("cucumber-deltaspike", "7.34.3"),
				cucumberWrapped("docstring", "7.34.3"),
				cucumberWrapped("cucumber-gherkin", "7.34.3"),
				cucumberWrapped("cucumber-gherkin-messages", "7.34.3"),
				cucumberWrapped("cucumber-guice", "7.34.3"),
				cucumberWrapped("cucumber-jakarta-cdi", "7.34.3"),
				cucumberWrapped("cucumber-java", "7.34.3"),
				cucumberWrapped("cucumber-java8", "7.34.3"),
				cucumberWrapped("cucumber-junit", "7.34.3"),
				cucumberWrapped("cucumber-junit-platform-engine", "7.34.3"),
				cucumberWrapped("cucumber-openejb", "7.34.3"),
				cucumberWrapped("cucumber-picocontainer", "7.34.3"),
				cucumberWrapped("cucumber-plugin", "7.34.3"),
				cucumberWrapped("cucumber-spring", "7.34.3"),
				cucumberWrapped("cucumber-testng", "7.34.3"));
	}

	private static ExpectedBundle cucumberWrapped(String artifactId, String version) {
		return generatedBundle("wrapped.io.cucumber." + artifactId, version, "io.cucumber:" + artifactId);
	}

	private static List<ExpectedBundle> bomTransitiveDependencyBundles() {
		return List.of(
				// Transitive deps pulled in by cucumber-junit-platform-engine
				originalOSGiBundle("junit-platform-commons", "1.14.2", "org.junit.platform:junit-platform-commons"),
				originalOSGiBundle("junit-platform-engine", "1.14.2", "org.junit.platform:junit-platform-engine"),
				originalOSGiBundle("org.apiguardian.api", "1.1.2", "org.apiguardian:apiguardian-api"),
				originalOSGiBundle("org.opentest4j", "1.3.0", "org.opentest4j:opentest4j"),
				// Transitive deps pulled in by cucumber-junit
				generatedBundle("wrapped.junit.junit", "4.13.2", "junit:junit"),
				generatedBundle("wrapped.org.hamcrest.hamcrest-core", "1.3.0", "org.hamcrest:hamcrest-core"),
				// Transitive deps pulled in by cucumber-picocontainer
				generatedBundle("wrapped.org.picocontainer.picocontainer", "2.15.2", "org.picocontainer:picocontainer"),
				// Transitive deps pulled in by cucumber-deltaspike
				generatedBundle("wrapped.org.apache.deltaspike.cdictrl.deltaspike-cdictrl-api", "1.9.6",
						"org.apache.deltaspike.cdictrl:deltaspike-cdictrl-api"),
				// Transitive deps pulled in by cucumber-testng
				originalOSGiBundle("org.testng", "7.12.0", "org.testng:testng"),
				originalOSGiBundle("jcommander", "1.83.0", "org.jcommander:jcommander", "1.83"),
				originalOSGiBundle("net.jodah.typetools", "0.6.3", "net.jodah:typetools"),
				originalOSGiBundle("org.hamcrest", "3.0.0", "org.hamcrest:hamcrest", "3.0"),
				originalOSGiBundle("slf4j.api", "2.0.16", "org.slf4j:slf4j-api"));
	}
}

