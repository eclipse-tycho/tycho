/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.TYCHO109product;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.jupiter.api.Test;

public class Tycho109ProductExportTest extends AbstractTychoIntegrationTest {

	@Test
	public void exportFeatureProduct() throws Exception {
		Verifier verifier = getVerifier("/TYCHO109product/feature-rcp");

		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		File basedir = new File(verifier.getBasedir());
		File output = new File(basedir, "HeadlessProduct/target/repository");
		File outputBinary = new File(output, "binary");

		assertTrue(output.isDirectory(), "Exported product folder not found\n" + output.getAbsolutePath());
		File launcher = new File(outputBinary, "HeadlessProduct.executable.gtk.linux.x86_64_1.0.0");
		assertTrue(launcher.isFile(), "Launcher not found\n" + launcher);

		File plugins = new File(output, "plugins");
		assertTrue(plugins.isDirectory(), "Plugins folder not found");

		File headlessPlugin = new File(plugins, "HeadlessPlugin_1.0.0.jar");
		assertTrue(headlessPlugin.isFile(), "Plugin should be unpacked");

		File features = new File(output, "features");
		assertTrue(features.isDirectory(), "Features folder not found");
	}

	@Test
	public void exportPluginRcpApplication() throws Exception {
		Verifier verifier = getVerifier("/TYCHO109product/plugin-rcp-app");

		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void productNoZip() throws Exception {
		Verifier verifier = getVerifier("/TYCHO109product/product-nozip/product");
		verifier.addCliOption("-Dtycho.product.createArchive=false");

		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		File basedir = new File(verifier.getBasedir());
		assertFalse(new File(basedir, "product/target/product-1.0.0.zip").canRead());
	}
}
