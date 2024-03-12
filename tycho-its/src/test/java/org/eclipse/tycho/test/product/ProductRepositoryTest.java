/*******************************************************************************
 * Copyright (c) 2024 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - [Issue #3522] product / repository contains source jars by default
 *******************************************************************************/
package org.eclipse.tycho.test.product;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class ProductRepositoryTest extends AbstractTychoIntegrationTest {

	@Test
	public void testShouldNotContainSources() throws Exception {
		Verifier verifier = getVerifier("product.productRepository", false);
		verifier.executeGoals(Arrays.asList("clean", "verify"));
		verifier.verifyErrorFreeLog();
		assertFileExists(new File(verifier.getBasedir()), //
				"target/repository/plugins/org.eclipse.osgi_*.jar");
		assertFileDoesNotExist(new File(verifier.getBasedir()), //
				"target/repository/plugins/org.eclipse.osgi.source_*.jar");
		assertFileExists(new File(verifier.getBasedir()), //
				"target/products/product.product/linux/gtk/x86_64/plugins/org.eclipse.osgi_*.jar");
		assertFileDoesNotExist(new File(verifier.getBasedir()), //
				"target/products/product.product/linux/gtk/x86_64/plugins/org.eclipse.osgi.source_*.jar");
	}

	@Test
	public void testShouldContainSourcesWhenExlicitlyIncluded() throws Exception {
		Verifier verifier = getVerifier("product.productRepository", false);
		verifier.addCliOption("-Pwithsources");
		verifier.executeGoals(Arrays.asList("clean", "verify"));
		verifier.verifyErrorFreeLog();
		assertFileExists(new File(verifier.getBasedir()), //
				"target/repository/plugins/org.eclipse.osgi_*.jar");
		assertFileExists(new File(verifier.getBasedir()), //
				"target/repository/plugins/org.eclipse.osgi.source_*.jar");
		assertFileExists(new File(verifier.getBasedir()), //
				"target/products/product.product/linux/gtk/x86_64/plugins/org.eclipse.osgi_*.jar");
		assertFileExists(new File(verifier.getBasedir()), //
				"target/products/product.product/linux/gtk/x86_64/plugins/org.eclipse.osgi.source_*.jar");
	}
}
