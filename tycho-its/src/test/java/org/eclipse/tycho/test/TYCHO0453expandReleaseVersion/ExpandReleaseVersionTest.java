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
package org.eclipse.tycho.test.TYCHO0453expandReleaseVersion;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.UpdateSite;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class ExpandReleaseVersionTest extends AbstractTychoIntegrationTest {
    @Test
    public void test() throws Exception {
        Verifier verifier = getVerifier("/TYCHO0453expandReleaseVersion", false);
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();

        File featureXml = new File(verifier.getBasedir(), "feature/target/feature.xml");
        Feature feature = Feature.read(featureXml);
        Assert.assertEquals("1.0.0.1234567890-bundle", feature.getPlugins().get(0).getVersion());
        // TODO included features

        File siteXml = new File(verifier.getBasedir(), "site/target/site/site.xml");
        UpdateSite site = UpdateSite.read(siteXml);
        Assert.assertEquals("1.0.0.1234567890-feature", site.getFeatures().get(0).getVersion());

        // TODO .product version expansion
    }

}
