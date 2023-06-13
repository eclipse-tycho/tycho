/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class OSGiRepositoryTest extends AbstractTychoIntegrationTest {

	@Test
	public void testProduceConsume() throws Exception {
		Verifier verifier = getVerifier("osgiRepository", false, true);
		verifier.executeGoals(asList("clean", "verify"));
		verifier.verifyErrorFreeLog();
		File repoFile = new File(verifier.getBasedir(), "site/target/repository/repository.xml.gz");
		assertTrue("repo file " + repoFile + " not found!", repoFile.exists());
		// TODO assert some things about the content
	}

}
