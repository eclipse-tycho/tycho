/*******************************************************************************
 * Copyright (c) 2012, 2018 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.test.target;

import java.io.File;
import java.nio.file.Files;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.eclipse.tycho.test.util.TargetDefinitionUtil;
import org.junit.Before;
import org.junit.Test;

public class TargetDefinitionPackagingTypeTest extends AbstractTychoIntegrationTest {

	private static final String TARGET_GROUPID = "tychoits.target.packagingType";
	private static final String TARGET_ARTIFACTID = "target-definition";
	private static final String TARGET_VERSION = "1.0.0-SNAPSHOT";
	private static final String TARGET_EXTENSION = "target";

	private Verifier verifier;
	private File targetDefinitionFile;

	@Before
	public void prepare() throws Exception {
		verifier = getVerifier("target.packagingType", false);
		// make sure target is not installed already
		verifier.deleteArtifact(TARGET_GROUPID, TARGET_ARTIFACTID, TARGET_VERSION, TARGET_EXTENSION);
		targetDefinitionFile = new File(verifier.getBasedir(), "target-definition/target-definition.target");
		TargetDefinitionUtil.setRepositoryURLs(targetDefinitionFile,
				ResourceUtil.P2Repositories.ECLIPSE_352.toString());
	}

	@Test
	public void testTargetDefinitionFromWithinReactor() throws Exception {
		verifier.addCliArgument("-PtargetAndBundle");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testTargetDefinitionFromLocalRepo() throws Exception {
		// first, install the target definition into the local repo
		verifier.addCliArgument("-PtargetOnly");
		verifier.executeGoal("install");
		verifier.verifyErrorFreeLog();
		verifier.verifyArtifactContent(TARGET_GROUPID, TARGET_ARTIFACTID, TARGET_VERSION, TARGET_EXTENSION,
				Files.readString(targetDefinitionFile.toPath()));
		// then, run the build of the bundle module only which should now
		// be able to resolve the target definition from the local repo
		verifier = getVerifier("target.packagingType", false);
		verifier.addCliArgument("-PbundleOnly");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

}
