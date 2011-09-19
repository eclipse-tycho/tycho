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

public class Tycho136GenerateIndividualSourceBundlesTest extends AbstractTychoIntegrationTest {

    @Test
    public void projectC() throws Exception {
        Verifier verifier = getVerifier("tycho136/projectC");

        verifier.executeGoal("install");
        verifier.verifyErrorFreeLog();

        File basedir = new File(verifier.getBasedir());
        File sourceFeature = new File(basedir, "SiteC/target/site/features/FeatureC.source_1.0.0.jar");
        Assert.assertTrue("Site should generate FeatureC.source", sourceFeature.exists());
        File sourcePluginC = new File(basedir, "SiteC/target/site/plugins/PluginC.source_1.0.0.jar");
        Assert.assertTrue("Site should generate PluginC.source", sourcePluginC.exists());
        File sourcePluginCExtra = new File(basedir, "SiteC/target/site/plugins/PluginC.Extra.source_1.0.0.jar");
        Assert.assertTrue("Site should generate PluginC.Extra.source", sourcePluginCExtra.exists());
    }
}
