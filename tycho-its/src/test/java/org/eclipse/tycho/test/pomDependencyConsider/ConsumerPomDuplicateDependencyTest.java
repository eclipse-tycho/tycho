/*******************************************************************************
 * Copyright (c) 2026 Faktor Zehn GmbH and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Faktor Zehn GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.pomDependencyConsider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.it.Verifier;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Reproduces a duplicate &lt;dependency&gt; entry in the generated consumer
 * POM when a dependency is both (a) declared/inherited as a plain Maven
 * {@code <dependency>} and (b) also resolved independently through
 * {@code pomDependencies=consider}. With {@code mapP2Dependencies=true},
 * {@code UpdateConsumerPomMojo} reverse-resolves the p2-flavored dependency
 * added by (b) back to the very same Maven coordinates as (a), but adds it
 * to the output model unconditionally, without checking whether a dependency
 * with the same resulting coordinates is already present. Both end up in
 * the generated {@code .tycho-consumer-pom.xml}.
 * <p>
 * This duplicate was always structurally present, but harmless, since Maven
 * &lt;=3.9 tolerated duplicate {@code <dependency>} declarations when
 * re-reading such a POM. Maven 3.10.0 made this a hard
 * "'dependencies.dependency.(groupId:artifactId:type:classifier)' must be
 * unique" validation error, breaking any tool that rebuilds the effective
 * model from the generated consumer POM (e.g. flatten-maven-plugin during a
 * {@code mavenCentralRelease}-style build).
 */
public class ConsumerPomDuplicateDependencyTest extends AbstractTychoIntegrationTest {

	@Test
	public void testNoDuplicateDependencyAfterMapP2DependenciesReverseResolution() throws Exception {
		Verifier verifier = getVerifier("/pomDependencyConsider.consumerPomDuplicate/artifact");
		verifier.executeGoal("install");
		verifier.verifyErrorFreeLog();

		verifier = getVerifier("/pomDependencyConsider.consumerPomDuplicate", false);
		verifier.executeGoal("install");
		verifier.verifyErrorFreeLog();

		DefaultModelReader reader = new DefaultModelReader();
		Model model = reader.read(new File(verifier.getBasedir(), "bundle/.tycho-consumer-pom.xml"), new HashMap<>());
		List<Dependency> dependencies = model.getDependencies();
		List<Dependency> artifactDependencies = dependencies.stream()
				.filter(d -> "consumerPomDuplicate".equals(d.getGroupId()) && "artifact".equals(d.getArtifactId()))
				.collect(Collectors.toList());
		assertEquals(1, artifactDependencies.size(),
				"'artifact' is declared as a plain (inherited) dependency AND is also resolved via "
						+ "pomDependencies=consider; mapP2Dependencies reverse-resolves the latter to the exact "
						+ "same Maven coordinates as the former, so it must only appear once in the generated "
						+ "consumer POM, but found: " + System.lineSeparator()
						+ artifactDependencies.stream().map(String::valueOf)
								.collect(Collectors.joining(System.lineSeparator())));
	}
}
