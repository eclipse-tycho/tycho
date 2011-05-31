/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.featurePatch.external;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class EclipseRepoIncludingFeaturePatchTest extends AbstractTychoIntegrationTest {

    @Test
    public void testRepsoitoryBuild() throws Exception {
        Verifier verifier = getVerifier("featurePatch.external/build", false);

        verifier.getSystemProperties().setProperty("ecl342", P2Repositories.ECLIPSE_342.toString());
        verifier.getSystemProperties().setProperty("ecl352", P2Repositories.ECLIPSE_352.toString());
        verifier.getSystemProperties().setProperty("repo-with-patch",
                ResourceUtil.resolveTestResource("projects/featurePatch.external/patchrepo").toURI().toString());

        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();

        // assert repository containing patched content
        File repositoryProjectFolder = new File(verifier.getBasedir(), "repository");
        P2RepositoryTool p2Repo = P2RepositoryTool.forEclipseRepositoryModule(repositoryProjectFolder);
        assertTrue(p2Repo.getBundleArtifact("org.eclipse.core.runtime", "3.5.0.v20090525").isFile());
    }
}
