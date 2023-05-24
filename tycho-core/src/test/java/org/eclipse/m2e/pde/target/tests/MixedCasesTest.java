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
public class MixedCasesTest extends AbstractMavenTargetTest {
	@Parameter(0)
	public Boolean includeSource;

	@Parameters(name = "includeSource={0}")
	public static List<Boolean> dependencyConfigurations() {
		return List.of(false, true);
	}

	@Test
	public void testMultipleArtifactsWithWrappingAndExclusion() throws Exception {
		ITargetLocation target = resolveMavenTarget(String
				.format(
				"""
						<location includeDependencyDepth="infinite" includeDependencyScopes="compile" includeSource="%s" missingManifest="generate" type="Maven">
							<dependencies>
								<dependency>
									<groupId>com.google.guava</groupId>
									<artifactId>failureaccess</artifactId>
									<version>1.0.1</version>
									<type>jar</type>
								</dependency>
								<dependency>
									<groupId>com.google.guava</groupId>
									<artifactId>guava</artifactId>
									<version>30.1.1-jre</version>
									<type>jar</type>
								</dependency>
								<dependency>
									<groupId>com.google.inject</groupId>
									<artifactId>guice</artifactId>
									<version>5.1.0</version>
									<type>jar</type>
								</dependency>
							</dependencies>
							<instructions><![CDATA[
								Bundle-Name:           Bundle derived from maven artifact ${mvnGroupId}:${mvnArtifactId}:${mvnVersion}
								version:               ${version_cleanup;${mvnVersion}}
								Bundle-SymbolicName:   m2e.wrapped.${mvnGroupId}.${mvnArtifactId}
								Bundle-Version:        ${version}
								Import-Package:        *
								Export-Package:        *;version="${version}";-noimport:=true
								-noextraheaders: true
							]]></instructions>
							<exclude>com.google.code.findbugs:jsr305:3.0.1</exclude>
							<exclude>com.google.code.findbugs:jsr305:3.0.2</exclude>
							<exclude>com.google.j2objc:j2objc-annotations:1.3</exclude>
							<exclude>org.checkerframework:checker-qual:3.5.0</exclude>
						</location>
						""",
				includeSource));
		assertStatusOk(target.getStatus());
		assertArrayEquals(EMPTY, target.getFeatures());
		List<ExpectedBundle> expectedBundles = List.of(//
				originalOSGiBundle("com.google.inject", "5.1.0", "com.google.inject:guice"),
				originalOSGiBundle("com.google.guava", "30.1.0.jre", "com.google.guava:guava", "30.1-jre"),
				originalOSGiBundle("com.google.guava", "30.1.1.jre", "com.google.guava:guava", "30.1.1-jre"),
				originalOSGiBundle("com.google.guava.failureaccess", "1.0.1", "com.google.guava:failureaccess"),
				originalOSGiBundle("org.objectweb.asm", "9.2.0", "org.ow2.asm:asm", "9.2"),
				originalOSGiBundle("checker-qual", "3.8.0", "org.checkerframework:checker-qual"),
				generatedBundle("m2e.wrapped.com.google.errorprone.error_prone_annotations", "2.3.4",
						"com.google.errorprone:error_prone_annotations"),
				generatedBundle("m2e.wrapped.com.google.errorprone.error_prone_annotations", "2.5.1",
						"com.google.errorprone:error_prone_annotations"),
				generatedBundle("m2e.wrapped.javax.inject.javax.inject", "1", "javax.inject:javax.inject"),
				generatedBundle("m2e.wrapped.aopalliance.aopalliance", "1.0", "aopalliance:aopalliance"),
				generatedBundle("m2e.wrapped.com.google.guava.listenablefuture",
						"9999.0.0.empty-to-avoid-conflict-with-guava", "com.google.guava:listenablefuture"));
		if (includeSource) {
			expectedBundles = withSourceBundles(expectedBundles).stream()
					.filter(e -> !"m2e.wrapped.com.google.guava.listenablefuture.source".equals(e.id())).toList();
			// com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava
			// doesn't have sources
		}
		assertTargetBundles(target, expectedBundles);
	}
}
