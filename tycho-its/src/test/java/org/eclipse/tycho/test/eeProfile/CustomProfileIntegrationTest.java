/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.eeProfile;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

public class CustomProfileIntegrationTest extends AbstractTychoIntegrationTest {

    @Test
    public void testBuildWithCustomProfile() throws Exception {
        // reactor with a test bundle importing javax.activation;version="1.1.0"
        Verifier verifier = getVerifier("eeProfile.custom/build", false);

        // repository where the custom EE is the only provider of javax.activation;version="1.1.0"
        verifier.setSystemProperty("custom-profile-repo",
                ResourceUtil.resolveTestResource("projects/eeProfile.custom/repository").toURI().toString());

        verifier.setSystemProperty("test-runtime-repo", ResourceUtil.P2Repositories.ECLIPSE_342.toString());
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();

        // custom EE is in build result (because there is a dependency from the product via the bundle and includeAllDependencies=true)
        P2RepositoryTool repo = P2RepositoryTool.forEclipseRepositoryModule(new File(verifier.getBasedir(), "product"));
        P2RepositoryTool.IU customProfileIU = repo.getUniqueIU("a.jre.customprofile");
        assertEquals("1.6.0", customProfileIU.getVersion());
    }
}
