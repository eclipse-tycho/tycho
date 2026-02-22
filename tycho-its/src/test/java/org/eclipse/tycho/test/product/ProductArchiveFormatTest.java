/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.product;

import java.io.File;
import java.util.List;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class ProductArchiveFormatTest extends AbstractTychoIntegrationTest {
	@Test
	public void test() throws Exception {
		Verifier verifier = getVerifier("/product.archiveFormat", true);
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();

		File basedir = new File(verifier.getBasedir());

		assertFileExists(basedir, "target/products/customArchiveName-win32.win32.x86.zip");
		assertFileExists(basedir, "target/products/customArchiveName-linux.gtk.x86.zip");
		assertFileExists(basedir, "target/products/customArchiveName-macosx.cocoa.x86_64.tar.gz");
	}

}
