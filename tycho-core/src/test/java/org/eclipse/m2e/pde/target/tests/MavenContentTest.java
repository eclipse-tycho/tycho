/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others
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

import org.eclipse.pde.core.target.ITargetLocation;
import org.junit.Test;

/**
 * Tests that the content of a location matches the expectation
 */
public class MavenContentTest extends AbstractMavenTargetTest {
	@Test
	public void testIncludeProvidedInfinite() throws Exception {
		ITargetLocation target = resolveMavenTarget(
				"""
						<location includeDependencyDepth="infinite" includeDependencyScopes="provided" includeSource="false" missingManifest="ignore" type="Maven">
							<dependencies>
								<dependency>
									<groupId>org.osgi</groupId>
									<artifactId>org.osgi.test.common</artifactId>
									<version>1.3.0</version>
									<type>jar</type>
								</dependency>
							</dependencies>
						</location>
						""");
		assertStatusOk(getTargetStatus(target));
		List<ExpectedBundle> expectedBundles = List.of( //
				originalOSGiBundle("osgi.annotation", "8.1.0.202202082230", "org.osgi:osgi.annotation", "8.1.0"),
				originalOSGiBundle("org.osgi.util.tracker", "1.5.4.202109301733", "org.osgi:org.osgi.util.tracker",
						"1.5.4"),
				originalOSGiBundle("org.osgi.test.common", "1.3.0", "org.osgi:org.osgi.test.common"),
				originalOSGiBundle("org.osgi.dto", "1.0.0.201505202023", "org.osgi:org.osgi.dto", "1.0.0"),
				originalOSGiBundle("org.osgi.framework", "1.8.0.201505202023", "org.osgi:org.osgi.framework", "1.8.0"),
				originalOSGiBundle("org.osgi.resource", "1.0.0.201505202023", "org.osgi:org.osgi.resource", "1.0.0"));
		assertTargetBundles(target, expectedBundles);
	}
}
