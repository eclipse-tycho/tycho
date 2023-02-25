/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.TYCHO0294ProductP2TargetPlatformResolver;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class ProductP2TargetPlatformResolverTest extends AbstractTychoIntegrationTest {
	@Test
	public void testBasic() throws Exception {
		Verifier verifier = getVerifier("/TYCHO0294ProductP2TargetPlatformResolver");
		verifier.addCliArgument("-Dp2.repo=" + P2Repositories.ECLIPSE_LATEST.toString());
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();

		File target = new File(verifier.getBasedir(), "product.bundle-based/target/repository/binary");

		assertFileExists(target, "product.bundle-based.executable.gtk.linux.x86_64_*");
		assertFileExists(target, "product.bundle-based.executable.cocoa.macosx.x86_64_*");
		assertFileExists(target, "product.bundle-based.executable.win32.win32.x86_64_*");
	}

}
