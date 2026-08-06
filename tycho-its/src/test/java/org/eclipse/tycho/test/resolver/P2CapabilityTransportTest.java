/*******************************************************************************
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Copilot - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.resolver;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

/**
 * Test for OSGi capability resolution with Provide-Capability and Require-Capability.
 * This test replicates the scenario from eclipse-equinox/p2 PR #972 where bundles
 * use OSGi capabilities to express service dependencies.
 * 
 * The test creates two bundles that form a direct dependency cycle, exactly as it
 * happens between org.eclipse.equinox.p2.repository and org.eclipse.equinox.p2.transport.ecf:
 * - consumer.bundle: Requires the Transport capability (like org.eclipse.equinox.p2.repository)
 * - provider.bundle: Provides that capability, but also Require-Bundle's consumer.bundle
 *   (like org.eclipse.equinox.p2.transport.ecf, which Require-Bundle's org.eclipse.equinox.p2.repository)
 * 
 * Since consumer.bundle requires the capability provided by provider.bundle, and
 * provider.bundle requires the consumer.bundle itself, the two bundles form a direct
 * dependency cycle that cannot be built as-is. To break the cycle during the reactor
 * build, consumer.bundle disables the Require-Capability requirement using the profile
 * property {@code org.eclipse.equinox.p2.disable.require.capability.osgi.implementation}
 * (see eclipse-equinox/p2#1073, replicated for Tycho's copy of BundlesAction and
 * additionally applied in Tycho's own OSGi state resolution used for classpath
 * computation). The published metadata and manifest of consumer.bundle are unaffected,
 * so the requirement remains active for a real (non-build-time) p2/OSGi runtime.
 * 
 * A third bundle, client.bundle, only Require-Bundle's consumer.bundle and does not
 * disable the capability requirement itself. Its dependency resolution therefore still
 * evaluates consumer.bundle's Require-Capability requirement (the disable-filter is only
 * scoped to the profile properties of the project being resolved, i.e. consumer.bundle's
 * own build), so client.bundle transitively pulls in provider.bundle as well. This is
 * verified indirectly via {@code org.eclipse.tycho.extras:tycho-dependency-tools-plugin:list-dependencies},
 * asserting that client.bundle's {@code target/dependencies-list.txt} lists both
 * consumer.bundle and provider.bundle.
 * 
 * @see <a href="https://github.com/eclipse-equinox/p2/pull/972">eclipse-equinox/p2#972</a>
 * @see <a href="https://github.com/eclipse-equinox/p2/issues/971">eclipse-equinox/p2#971</a>
 * @see <a href="https://github.com/eclipse-equinox/p2/pull/1073">eclipse-equinox/p2#1073</a>
 */
public class P2CapabilityTransportTest extends AbstractTychoIntegrationTest {

	@Test
	public void testCapabilityProvideRequireCycleBroken() throws Exception {
		Verifier verifier = getVerifier("/p2.capability.transport");
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();

		File file = new File(verifier.getBasedir(), "client.bundle/target/dependencies-list.txt");
		assertTrue("dependencies-list.txt was not generated for client.bundle", file.exists());
		List<String> fileNames = new ArrayList<>();
		try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.isBlank()) {
					fileNames.add(new File(line).getName());
				}
			}
		}
		assertTrue("consumer.bundle jar not found in client.bundle dependencies: " + fileNames,
				fileNames.stream().anyMatch(name -> name.startsWith("consumer.bundle-")));
		assertTrue("provider.bundle jar not found in client.bundle dependencies: " + fileNames,
				fileNames.stream().anyMatch(name -> name.startsWith("provider.bundle-")));
	}

}
