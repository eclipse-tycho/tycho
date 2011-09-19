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
package org.eclipse.tycho.test.tycho136;

import java.io.File;

import junit.framework.Assert;

import org.apache.maven.it.Verifier;
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
