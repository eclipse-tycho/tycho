/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.TYCHO0420multipleRepositoryPerLocationTarget;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.model.Target;
import org.eclipse.tycho.model.Target.Repository;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class MultipleRepositoryPerLocationTargetTest extends AbstractTychoIntegrationTest {
    @Test
    public void testDependenciesOfUnitResolvedFromTwoRepositories() throws Exception {
        Verifier verifier = getVerifier("/TYCHO0420multipleRepositoryPerLocationTarget", false);

        File targetPlatformFile = new File(verifier.getBasedir(), "target-platform/tycho420.target");
        makeRepositoryURLsAbsolute(targetPlatformFile);

        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
    }

    static void makeRepositoryURLsAbsolute(File dotTargetFile) throws Exception {
        Target target = Target.read(dotTargetFile);

        for (Target.Location location : target.getLocations()) {
            for (Repository repository : location.getRepositories()) {
                File file = new File(repository.getLocation());
                repository.setLocation(file.getCanonicalFile().toURI().toASCIIString());
            }
        }

        Target.write(target, dotTargetFile);
    }
}
