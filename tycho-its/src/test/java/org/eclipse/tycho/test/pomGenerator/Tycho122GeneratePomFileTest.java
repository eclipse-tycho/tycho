/*******************************************************************************
 * Copyright (c) 2008, 2018 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.pomGenerator;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.core.utils.TychoVersion;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class Tycho122GeneratePomFileTest extends AbstractTychoIntegrationTest {
    @Test
    public void generatePom() throws Exception {
        Verifier verifier = getVerifier("/pomGenerator/tycho.demo", false);

        verifier.setAutoclean(false);
        verifier.executeGoal(
                "org.eclipse.tycho:tycho-pomgenerator-plugin:" + TychoVersion.getTychoVersion() + ":generate-poms");
        verifier.verifyErrorFreeLog();

        File pom = new File(verifier.getBasedir(), "pom.xml");
        assertTrue("Must generate the pom.xml", pom.exists());

        // TODO the content of the pom.xml is broken (bug 363908)
    }

}
