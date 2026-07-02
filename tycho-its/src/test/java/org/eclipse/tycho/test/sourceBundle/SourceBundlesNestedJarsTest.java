/*******************************************************************************
 * Copyright (c) 2013, 2021 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.sourceBundle;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.jar.JarFile;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class SourceBundlesNestedJarsTest extends AbstractTychoIntegrationTest {

	@Test
	public void testDistinctSourceRoots() throws Exception {
		Verifier verifier = getVerifier("sourceBundle.nestedJars", false);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
		File sourceJar = new File(verifier.getBasedir(), "target/test.distinct.sourceroots-1.0.0-sources.jar");
		assertTrue(sourceJar.isFile());
		try (JarFile jar = new JarFile(sourceJar)) {
			String sourceBundleHeader = jar.getManifest().getMainAttributes().getValue("Eclipse-SourceBundle");
			ManifestElement element = ManifestElement.parseHeader("", sourceBundleHeader)[0];
			String[] roots = element.getDirective("roots").split(",");
			assertEquals(new HashSet<>(asList(".", "foosrc", "barsrc")), new HashSet<>(asList(roots)));
			assertNotNull(jar.getEntry("Main.java"));
			assertNotNull(jar.getEntry("foosrc/Foo1.java"));
			assertNotNull(jar.getEntry("foosrc/Foo2.java"));
			assertNotNull(jar.getEntry("barsrc/Bar1.java"));
			assertNotNull(jar.getEntry("barsrc/Bar2.java"));
		}
	}
}
