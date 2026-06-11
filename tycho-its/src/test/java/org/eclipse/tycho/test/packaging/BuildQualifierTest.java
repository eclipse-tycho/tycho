/*******************************************************************************
 * Copyright (c) 2022 Hannes Wellmann and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.test.packaging;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class BuildQualifierTest extends AbstractTychoIntegrationTest {

	@Test
	public void testBuildQualifierMojoWithJGitTimestamp_jarPackagingType() throws Exception {
		Verifier verifier = getVerifier("/packaging.build-qualifier/jar-packaging", false);
		verifier.executeGoal("generate-sources");
		verifier.verifyErrorFreeLog();

		Path basedir = Path.of(verifier.getBasedir());
		Path buildQualifierFile = basedir.resolve(Path.of("target", "build-qualifier"));
		assertNotEquals("", Files.readString(buildQualifierFile));
	}

}
