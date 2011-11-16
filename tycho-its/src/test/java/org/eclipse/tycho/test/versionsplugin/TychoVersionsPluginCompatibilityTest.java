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

public class TychoVersionsPluginCompatibilityTest extends AbstractTychoIntegrationTest {

    /**
     * <p>
     * This test verifies that current and future versions of the tycho-versions-plugin can be
     * executed on a project that is built with Tycho 0.12.0. With this assertion it's possible to
     * call the plugin without version on the commandline:
     * </p>
     * <p>
     * <code>mvn org.eclipse.tycho:tycho-versions-plugin:set-version</code>
     * </p>
     * <p>
     * Background: The tycho-versions-plugin 0.12.0 can't handle projects that are built with Tycho
     * 0.11.0 or older, see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=363791">Bug
     * 363791</a>.
     * </p>
     */
    @Test
    public void invokeVersionsPluginOnTycho0120Project() throws Exception {
        String expectedNewVersion = "1.2.3";

        Verifier verifier = getVerifier("TychoVersionsPluginTest", true);

        verifier.getCliOptions().add("-DnewVersion=" + expectedNewVersion);
        verifier.executeGoal("org.eclipse.tycho:tycho-versions-plugin:" + TychoVersion.getTychoVersion()
                + ":set-version");

        verifier.verifyErrorFreeLog();

        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model pomModel = pomReader.read(new FileReader(new File(verifier.getBasedir(), "pom.xml")));
        assertEquals("<version> in pom.xml has not been changed!", expectedNewVersion, pomModel.getVersion());
    }

}
