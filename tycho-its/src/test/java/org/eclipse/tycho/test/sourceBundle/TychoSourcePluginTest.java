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
import static org.junit.Assert.assertTrue;

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
import org.junit.Test;

public class TychoSourcePluginTest extends AbstractTychoIntegrationTest {

	@Test
	public void testBasic() throws Exception {
		Verifier verifier = getVerifier("/sourcePlugin/basic", false, false);
		verifier.addCliOption("-De342-url=" + ECLIPSE_342.toString());
		verifier.executeGoals(List.of("clean", "install"));
		verifier.verifyErrorFreeLog();
		File feature = new File(verifier.getBasedir(),
				"sourcefeature.repository/target/repository/features/sourcefeature.feature_1.0.0.123abc.jar");
		assertTrue("Missing expected file " + feature, feature.canRead());

		try (ZipFile featureZip = new ZipFile(feature)) {
			assertTrue("Missing expected file featrue.properties in " + feature,
					findEntry(featureZip, "feature.properties").isPresent());

			// Test for bug 552066
			assertTrue("license.html not found in " + feature, findEntry(featureZip, "license.html").isPresent());
			assertTrue("bin-only.txt not found in " + feature, findEntry(featureZip, "bin-only.txt").isPresent());
			assertTrue("src-only.txt found in " + feature, findEntry(featureZip, "src-only.txt").isEmpty());
		}

	}

	private static Optional<ZipEntry> findEntry(ZipFile zip, String name) {
		Stream<ZipEntry> stream = StreamSupport
				.stream(Spliterators.spliteratorUnknownSize(zip.entries().asIterator(), Spliterator.ORDERED), false);
		return stream.filter(e -> e.getName().equals(name)).findAny();
	}

	@Test
	public void testExtraSourceBundles() throws Exception {
		Verifier verifier = getVerifier("/sourcePlugin/extra-source-bundles", false, false);
		verifier.addCliOption("-De342-url=" + ECLIPSE_342.toString());
		verifier.executeGoals(List.of("clean", "install"));
		verifier.verifyErrorFreeLog();
		File file = new File(verifier.getBasedir(),
				"sourcefeature.repository/target/repository/plugins/extra.sourcefeature.bundle_1.0.0.123abc.jar");
		assertTrue("Missing expected file", file.canRead());
	}

	@Test
	public void testRemoteSourceBundles() throws Exception {
		Verifier verifier = getVerifier("/sourcePlugin/remote-source-bundles", false, false);
		verifier.addCliOption("-De342-url=" + ECLIPSE_342.toString());
		verifier.executeGoals(List.of("clean", "install"));
		verifier.verifyErrorFreeLog();
		File file = new File(verifier.getBasedir(),
				"sourcefeature.repository/target/repository/plugins/org.junit.source_3.8.2.v3_8_2_v20100427-1100.jar");
		assertTrue("Missing expected file " + file.getName(), file.canRead());
		file = new File(verifier.getBasedir(),
				"sourcefeature.repository/target/repository/plugins/org.junit.source_4.8.1.v4_8_1_v20100427-1100.jar");
		assertTrue("Missing expected file " + file.getName(), file.canRead());

	}
}
