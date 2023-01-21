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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.packaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.it.Verifier;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Ignore;
import org.junit.Test;

public class ConsumerPomTest extends AbstractTychoIntegrationTest {
	DefaultModelReader reader = new DefaultModelReader();

	@Test
	@Ignore("Disabled because of maven central outages")
	public void testReplaceP2() throws Exception {
		Verifier verifier = getVerifier("packaging.consumer.pom", true);
		verifier.addCliOption("-U");
		verifier.addCliOption("-DmapDependencies=true");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
		DefaultModelReader reader = new DefaultModelReader();
		Model model = reader.read(new File(verifier.getBasedir(), "bundle/.tycho-consumer-pom.xml"), new HashMap<>());
		List<Dependency> dependencies = model.getDependencies();
		assertHasDependency("org.eclipse.platform", "org.eclipse.equinox.preferences", dependencies);
		assertHasDependency("org.eclipse.platform", "org.eclipse.equinox.registry", dependencies);
		assertHasDependency("org.eclipse.platform", "org.eclipse.equinox.common", dependencies);
		assertHasDependency("org.eclipse.platform", "org.eclipse.equinox.app", dependencies);
		assertHasDependency("org.osgi", "org.osgi.service.prefs", dependencies);
	}

	@Test
	public void testReplacePackagingType() throws Exception {
		Verifier verifier = getVerifier("packaging.consumer.pom", true);
		verifier.addCliOption("-U");
		verifier.addCliOption("-DmapDependencies=false");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
		DefaultModelReader reader = new DefaultModelReader();
		Model model = reader.read(new File(verifier.getBasedir(), "bundle/.tycho-consumer-pom.xml"), new HashMap<>());
		assertEquals("packaging was not replaced", "jar", model.getPackaging());
	}

	private void assertHasDependency(String g, String a, List<Dependency> dependencies) {
		Optional<Dependency> findAny = dependencies.stream()
				.filter(d -> g.equals(d.getGroupId()) && a.equals(d.getArtifactId())).findAny();
		if (!findAny.isPresent()) {
			fail("dependency with groupId=" + g + " and artifactId=" + a + " not found in " + System.lineSeparator()
					+ dependencies.stream().map(String::valueOf).collect(Collectors.joining(System.lineSeparator())));
		}

	}
}