/*******************************************************************************
 * Copyright (c) 2026 Andrew Tram and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrew Tram - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.target;

import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Tests the <code>p2MavenMetadataHandling</code> parameter of the
 * target-platform-configuration. The project depends on a bundle coming from a Maven target
 * location (commons-io), so its p2 unit carries Maven coordinates. Whether these coordinates are
 * used for the project dependency is visible in the output of <code>dependency:tree</code> after the
 * project was compiled: mapped coordinates show up as a regular <code>compile</code> dependency, unmapped ones as a
 * <code>system</code> scoped <code>p2.*</code> dependency. Only the <code>[INFO]</code> tree lines are
 * matched, as the forked build runs with debug output that lists unrelated dependencies.
 */
public class P2MavenMetadataHandlingTest extends AbstractTychoIntegrationTest {

	private static final String TREE_PREFIX = "[INFO] \\- ";
	private static final String MAPPED_DEPENDENCY = TREE_PREFIX + "commons-io:commons-io:jar:2.11.0:compile";
	private static final String UNMAPPED_DEPENDENCY = TREE_PREFIX + "p2.eclipse.plugin:org.apache.commons.commons-io:eclipse-plugin:2.11.0:system";

	@Test
	public void validateMapsCoordinates() throws Exception {
		Verifier verifier = buildWithHandling("validate");
		verifier.verifyTextInLog(MAPPED_DEPENDENCY);
		verifyTextNotInLog(verifier, UNMAPPED_DEPENDENCY);
	}

	@Test
	public void injectMapsCoordinatesWithoutRemoteResolution() throws Exception {
		Verifier verifier = buildWithHandling("inject");
		verifier.verifyTextInLog(MAPPED_DEPENDENCY);
		verifyTextNotInLog(verifier, UNMAPPED_DEPENDENCY);
		verifyTextNotInLog(verifier, "Mapping P2 > Maven Coordinates failed");
	}

	@Test
	public void ignoreSkipsMapping() throws Exception {
		Verifier verifier = buildWithHandling("ignore");
		verifier.verifyTextInLog(UNMAPPED_DEPENDENCY);
		verifyTextNotInLog(verifier, MAPPED_DEPENDENCY);
		verifyTextNotInLog(verifier, "Mapping P2 > Maven Coordinates failed");
	}

	@Test
	public void illegalValueFailsBuild() throws Exception {
		Verifier verifier = getVerifier("target.p2MavenMetadataHandling", false, true);
		verifier.addCliOption("-Dp2MavenMetadataHandling=bogus");
		try {
			verifier.executeGoals(List.of("compile", "dependency:tree"));
		} catch (Exception e) {
			// expected, the build must fail
		}
		verifier.verifyTextInLog("Illegal value of <p2MavenMetadataHandling>");
	}

	private Verifier buildWithHandling(String handling) throws Exception {
		Verifier verifier = getVerifier("target.p2MavenMetadataHandling", false, true);
		verifier.addCliOption("-Dp2MavenMetadataHandling=" + handling);
		verifier.executeGoals(List.of("compile", "dependency:tree"));
		verifier.verifyErrorFreeLog();
		return verifier;
	}
}
