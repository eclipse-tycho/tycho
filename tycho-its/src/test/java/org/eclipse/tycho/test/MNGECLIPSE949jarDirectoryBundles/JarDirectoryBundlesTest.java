/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.MNGECLIPSE949jarDirectoryBundles;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileFilter;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.core.osgitools.DefaultBundleReader;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class JarDirectoryBundlesTest extends AbstractTychoIntegrationTest {

	@Test
	public void test() throws Exception {
		Verifier verifier = getVerifier("MNGECLIPSE949jarDirectoryBundles");

		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		File[] sitePlugins = new File(verifier.getBasedir(), "site/target/repository/plugins").listFiles(
				(FileFilter) pathname -> pathname.isFile() && pathname.getName().startsWith("org.eclipse.platform")
						&& pathname.getName().endsWith(".jar"));
		assertEquals(1, sitePlugins.length);

		// verify the bundle actually makes sense
		DefaultBundleReader reader = new DefaultBundleReader();
		OsgiManifest siteBundleManifest = reader.loadManifest(sitePlugins[0]);
		assertEquals("platform.jar", siteBundleManifest.getBundleClasspath()[0]);
		assertEquals("org.eclipse.platform", siteBundleManifest.getBundleSymbolicName());

		File[] productPlugins = new File(verifier.getBasedir(), "product/target/repository/plugins")
				.listFiles((FileFilter) pathname -> pathname.getName().startsWith("org.eclipse.platform"));
		assertEquals(1, productPlugins.length);

		// verify directory actually makes sense
		OsgiManifest productBundleManifest = reader.loadManifest(productPlugins[0]);
		assertEquals("platform.jar", productBundleManifest.getBundleClasspath()[0]);
		assertEquals("org.eclipse.platform", productBundleManifest.getBundleSymbolicName());

	}

}
