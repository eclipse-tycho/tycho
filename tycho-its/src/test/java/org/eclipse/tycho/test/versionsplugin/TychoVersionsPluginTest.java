/*******************************************************************************
 * Copyright (c) 2012 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.versionsplugin;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileReader;

import org.apache.maven.it.Verifier;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.eclipse.tycho.core.utils.TychoVersion;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

/**
 * 
 * @author mistria
 * 
 */
public class TychoVersionsPluginTest extends AbstractTychoIntegrationTest {

    @Test
    public void testVersionBump() throws Exception {
        Verifier verifier = getVerifier("TychoVersionsPluginTest_multiple_versions", true);

        verifier.getCliOptions().add("-DversionDiff=0.0.1");
        verifier.executeGoal("org.eclipse.tycho:tycho-versions-plugin:" + TychoVersion.getTychoVersion()
                + ":bump-version");

        verifier.verifyErrorFreeLog();

        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        // parent
        Model pomModel = pomReader.read(new FileReader(new File(verifier.getBasedir(), "pom.xml")));
        assertEquals("<version> in pom.xml has not been changed!", "1.0.1-SNAPSHOT", pomModel.getVersion());
        // bundle
        pomModel = pomReader.read(new FileReader(new File(verifier.getBasedir(), "bundle/pom.xml")));
        assertEquals("<version> in bundle/pom.xml has not been changed!", "2.0.1-SNAPSHOT", pomModel.getVersion());
        // feature 1.0.1
        pomModel = pomReader.read(new FileReader(new File(verifier.getBasedir(), "feature101/pom.xml")));
        assertEquals("<version> in feature101/pom.xml has not been changed!", "1.0.2-SNAPSHOT", pomModel.getVersion());
        // feature 1.1.0
        pomModel = pomReader.read(new FileReader(new File(verifier.getBasedir(), "feature110/pom.xml")));
        assertEquals("<version> in feature110/pom.xml has not been changed!", "1.1.1-SNAPSHOT", pomModel.getVersion());
    }
}
