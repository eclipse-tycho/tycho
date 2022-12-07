/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.tycho136;

import java.io.File;

import org.junit.Assert;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class Tycho136GeneratePluginTest extends AbstractTychoIntegrationTest {

    @Test
    public void projectB() throws Exception {
        Verifier verifier = getVerifier("tycho136/projectB");

        verifier.executeGoal("install");
        verifier.verifyErrorFreeLog();

        File basedir = new File(verifier.getBasedir());
        File sourcePlugin = new File(basedir, "SiteB/target/site/plugins/PluginB.source_1.0.0.jar");
        Assert.assertTrue("Site should generate PluginB.source", sourcePlugin.exists());
    }
}
