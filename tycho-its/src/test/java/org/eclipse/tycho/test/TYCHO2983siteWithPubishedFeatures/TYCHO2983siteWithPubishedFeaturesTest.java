/*******************************************************************************
 * Copyright (c) 2010, 2023 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.test.TYCHO2983siteWithPubishedFeatures;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

import static org.junit.Assert.*;

public class TYCHO2983siteWithPubishedFeaturesTest extends AbstractTychoIntegrationTest {

    @Test
    public void testCheckSiteFeatures() throws Exception {
        Verifier verifier = getVerifier("/TYCHO2983siteWithPubishedFeatures", false);
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();

        File siteFeaturesFolder = new File(verifier.getBasedir(), "helloworld.updatesite/target/repository/features");

        assertTrue(siteFeaturesFolder.exists());
        assertNotNull(Arrays.stream(Objects.requireNonNull(siteFeaturesFolder.listFiles())).filter(
                file -> file.getName().startsWith("helloworld.feature"))
                .findAny()
                .orElse(null)
        );
        assertNotNull(Arrays.stream(Objects.requireNonNull(siteFeaturesFolder.listFiles())).filter(
                file -> file.getName().startsWith("io.spring.javaformat.eclipse.feature"))
                .findAny()
                .orElse(null)
        );
    }
}
