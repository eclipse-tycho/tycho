/*******************************************************************************
 * Copyright (c) 2015 Rapicorp, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Rapicorp, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.iu;

import java.io.File;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.model.IU;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

import de.pdark.decentxml.Element;

public class IUMetadataGenerationTest extends AbstractTychoIntegrationTest {

    @Test
    public void testIUWithArtifact() throws Exception {
        Verifier verifier = getVerifier("iu.artifact", false);

        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();

        File target = new File(verifier.getBasedir(), "target");
        File p2artifacts = new File(target, "p2artifacts.xml");
        File p2content = new File(target, "p2content.xml");

        Assert.assertTrue(p2artifacts.canRead());
        Assert.assertTrue(p2content.canRead());

        //Here we check that the final IU contained in the p2content.xml has the right shape
        IU finalIU = IU.read(p2content);
        Assert.assertNotNull(finalIU.getSelfCapability());
        Assert.assertEquals("iuA", finalIU.getSelfCapability().getAttributeValue(IU.NAME));
        Assert.assertEquals("1.0.0", finalIU.getSelfCapability().getAttributeValue(IU.VERSION));

        Assert.assertEquals("iuA", finalIU.getSelfArtifact().getAttributeValue(IU.ID));
        Assert.assertEquals("1.0.0", finalIU.getSelfArtifact().getAttributeValue(IU.VERSION));

        //check properties
        assertMavenProperties(finalIU);

        //check that the artifact is here
        Assert.assertTrue(new File(target, "iuA-1.0.0.zip").exists());
    }

    private void assertMavenProperties(IU iu) {
        final String MAVEN_ARTIFACT_ID = "maven-artifactId";
        final String MAVEN_VERSION = "maven-version";
        final String MAVEN_GROUP_ID = "maven-groupId";

        List<Element> properties = iu.getProperties();
        int count = 0;
        if (properties != null) {
            for (Element property : properties) {
                String key = property.getAttributeValue("name");
                if (MAVEN_GROUP_ID.equals(key))
                    count += 1;
                if (MAVEN_ARTIFACT_ID.equals(key))
                    count += 2;
                if (MAVEN_VERSION.equals(key))
                    count += 4;
            }
        }
        Assert.assertTrue("Maven properties can't be found", count == 7);
    }

    @Test
    public void testIUWithoutArtifact() throws Exception {
        Verifier verifier = getVerifier("iu.withoutArtifact", false);

        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();

        File target = new File(verifier.getBasedir(), "target");
        File p2content = new File(target, "p2content.xml");

        Assert.assertTrue(p2content.canRead());

        //Here we check that the final IU contained in the p2content.xml has the right shape
        IU finalIU = IU.read(p2content);
        Assert.assertNotNull(finalIU.getSelfCapability());
        Assert.assertEquals("iuA", finalIU.getSelfCapability().getAttributeValue(IU.NAME));
        Assert.assertEquals("1.0.0", finalIU.getSelfCapability().getAttributeValue(IU.VERSION));

        Assert.assertNull(finalIU.getSelfArtifact());

    }
}
