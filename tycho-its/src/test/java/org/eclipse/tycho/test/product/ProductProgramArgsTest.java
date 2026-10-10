/*******************************************************************************
 * Copyright (c) 2023 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Quang Tran - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.product;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class ProductProgramArgsTest extends AbstractTychoIntegrationTest {
	@Test
	public void test() throws Exception {
		Verifier verifier = getVerifier("/product.programArgs", true);
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();

		File basedir = new File(verifier.getBasedir());
		File productDir = new File(basedir, "target/products/ppa.product/win32/win32/x86");

		assertFileExists(productDir, "eclipse.ini");

		File initFile = new File(productDir, "eclipse.ini");

		List<String> lines = Files.readAllLines(initFile.toPath());
		assertTrue(lines.indexOf("-blah1") >= 0);
		assertTrue(lines.indexOf("-blah2") >= 0);

		int configurationLineIndex = lines.indexOf("-configuration");
		assertTrue(configurationLineIndex >= 0);
		assertEquals(configurationLineIndex + 1, lines.indexOf("@user.dir/configuration"));
	}

}
