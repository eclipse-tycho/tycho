/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class P2RepositoryValidateTest extends AbstractTychoIntegrationTest {

	@Test
	public void testValidate() throws Exception {
		Verifier verifier = getVerifier("p2Repository.unresolvableIU", false);
		verifier.addCliArgument("-Dtest-data-repo=" + P2Repositories.ECLIPSE_352.toString());
		try {
			// validate is not always enough here as only in the prepare-package there is
			// the first mojo that requires classpath resolving and we want to delay it
			// until there...
			verifier.executeGoal("prepare-package");
			fail("Expected build failure");
		} catch (VerificationException ex) {
			// expected
		}
		verifier.verifyTextInLog("non.existing.iu");
	}

	@Test
	public void testValidateDoesNotFetch() throws Exception {
		Verifier verifier = getVerifier("p2Repository.basic", false);
		verifier.addCliArgument("-Dtest-data-repo=" + P2Repositories.ECLIPSE_352.toString());
		File bundleCopyFolder = new File(verifier.getLocalRepository(),
				"p2/osgi/bundle/org.eclipse.osgi/3.5.2.R35x_v20100126"); // relative path should use some API
		if (bundleCopyFolder.exists()) {
			Arrays.stream(bundleCopyFolder.listFiles()).forEach(File::delete);
			bundleCopyFolder.delete();
		}
		verifier.executeGoal("validate");
		verifier.verifyErrorFreeLog();
		assertFalse("Bundle shouldn't be copied locally just for validate", bundleCopyFolder.exists());
	}
}
