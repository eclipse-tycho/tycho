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
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.sourceBundle;

import static org.eclipse.tycho.test.util.ResourceUtil.P2Repositories.ECLIPSE_342;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.jupiter.api.Test;

public class TychoSourcePluginTest extends AbstractTychoIntegrationTest {

	@Test
	public void testBasic() throws Exception {
		Verifier verifier = getVerifier("/sourcePlugin/basic", false, false);
		verifier.addCliOption("-De342-url=" + ECLIPSE_342.toString());
		verifier.executeGoals(List.of("clean", "install"));
		verifier.verifyErrorFreeLog();
		File feature = new File(verifier.getBasedir(),
				"sourcefeature.repository/target/repository/features/sourcefeature.feature_1.0.0.123abc.jar");
		assertTrue(feature.canRead(), "Missing expected file " + feature);

		try (ZipFile featureZip = new ZipFile(feature)) {
			assertTrue(findEntry(featureZip, "feature.properties").isPresent(),
					"Missing expected file featrue.properties in " + feature);

			// Test for bug 552066
			assertTrue(findEntry(featureZip, "license.html").isPresent(), "license.html not found in " + feature);
			assertTrue(findEntry(featureZip, "bin-only.txt").isPresent(), "bin-only.txt not found in " + feature);
			assertTrue(findEntry(featureZip, "src-only.txt").isEmpty(), "src-only.txt found in " + feature);
		}
	}

	private static Optional<ZipEntry> findEntry(ZipFile zip, String name) {
		Stream<ZipEntry> stream = StreamSupport
				.stream(Spliterators.spliteratorUnknownSize(zip.entries().asIterator(), Spliterator.ORDERED), false);
		return stream.filter(e -> e.getName().equals(name)).findAny();
	}

	@Test
	public void testRemoteSourceBundles() throws Exception {
		Verifier verifier = getVerifier("/sourcePlugin/remote-source-bundles", true, false);
		verifier.executeGoals(List.of("clean", "install"));
		verifier.verifyErrorFreeLog();
		File file = new File(verifier.getBasedir(),
				"sourcefeature.repository/target/repository/plugins/org.junit.source_4.13.2.v20240929-1000.jar");
		assertTrue(file.canRead(), "Missing expected file " + file.getName());
		file = new File(verifier.getBasedir(),
				"sourcefeature.repository/target/repository/plugins/org.hamcrest.core.source_2.2.0.v20230809-1000.jar");
		assertTrue(file.canRead(), "Missing expected file " + file.getName());

	}
}
