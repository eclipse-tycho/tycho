/*******************************************************************************
 * Copyright (c) 2021, 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - [Issue #80] Incorrect requirement version for configuration/plugins in publish-products
 *******************************************************************************/
package org.eclipse.tycho.test.product;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class ProductMixedVersionsTest extends AbstractTychoIntegrationTest {
	@Test
	public void testMixedPluginVersions() throws Exception {
		Verifier verifier = getVerifier("product.differentVersions", false);
		verifier.addCliArgument("-Dplatform-url=" + P2Repositories.ECLIPSE_LATEST.toString());
		verifier.executeGoals(Arrays.asList("clean", "verify"));
		verifier.verifyErrorFreeLog();
		// check that simple configurator is there...
		File product = new File(verifier.getBasedir(), "product/target/products/com.test.sample.product");
		File[] bundleInfoFiles = assertFileExists(product,
				"**/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info");
		for (String bundleInfo : Files.readAllLines(bundleInfoFiles[0].toPath(), StandardCharsets.UTF_8)) {
			String[] parts = bundleInfo.split(",");
			if (parts.length == 5 && "org.apache.activemq.activemq-core".equals(parts[0])) {
				assertEquals("Version of activemq bundle does not match", "5.2.0", parts[1]);
				assertEquals("Start level of activemq bundle does not match", "3", parts[3]);
				assertEquals("Autostart of activemq bundle does not match", "true", parts[4]);
			}
			return;
		}
		fail();
	}
}
