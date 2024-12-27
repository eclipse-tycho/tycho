/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test;

import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.P2RepositoryTool.IU;
import org.eclipse.tycho.test.util.P2RepositoryTool.IdAndVersion;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

public class P2RepositoryPluginTest extends AbstractTychoIntegrationTest {

	@Test
	public void testP2RemoveUI() throws Exception {
		Verifier verifier = getVerifier("/p2Repository.removeUI");
		verifier.addCliOption("-Dtest-data-repo=" + ResourceUtil.P2Repositories.ECLIPSE_352);
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		P2RepositoryTool p2Repo = P2RepositoryTool.forEclipseRepositoryModule(new File(verifier.getBasedir()));
		List<IdAndVersion> allUnits = p2Repo.getAllUnits();
		for (IdAndVersion idAndVersion : allUnits) {
			IU iu = p2Repo.getIU(idAndVersion.id(), idAndVersion.version());
			for (String prop : iu.getProperties()) {
				assertNotEquals("org.eclipse.equinox.p2.name=Uncategorized", prop);
			}
		}
	}

}
