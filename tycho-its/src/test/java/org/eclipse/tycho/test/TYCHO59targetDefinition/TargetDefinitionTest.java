/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.TYCHO59targetDefinition;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.model.Target;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class TargetDefinitionTest extends AbstractTychoIntegrationTest {
    @Test
    public void testMultiplatformReactorBuild() throws Exception {
        Verifier verifier = getVerifier("/TYCHO59targetDefinition");

        File platformFile = new File(verifier.getBasedir(), "target-platform/platform.target");
        Target platform = Target.read(platformFile);

        for (Target.Location location : platform.getLocations()) {
            Target.Repository repository = location.getRepositories().get(0);
            File file = new File(repository.getLocation());
            repository.setLocation(file.getCanonicalFile().toURI().toASCIIString());
        }

        Target.write(platform, platformFile);

        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
    }

}
