/*******************************************************************************
 * Copyright (c) 2014 Bachmann electronics GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Bachmann electronics GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.shared.BuildFailureException;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;
import org.junit.Test;

public class DefaultTargetPlatformConfigurationReaderTest extends AbstractTychoMojoTestCase {

    private DefaultTargetPlatformConfigurationReader configurationReader;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        configurationReader = lookup(DefaultTargetPlatformConfigurationReader.class);
    }

    @Override
    protected void tearDown() throws Exception {
        configurationReader = null;
        super.tearDown();
    }

    @Test
    public void testExtraRequirementMissingVersionRange() throws Exception {
        Xpp3Dom dom = createConfigurationDom("type", "id");
        try {
            configurationReader.readExtraRequirements(new TargetPlatformConfiguration(), dom);
            fail();
        } catch (BuildFailureException e) {
            assertTrue(e.getMessage().contains(
                    "Element <versionRange> is missing in <extraRequirements><requirement> section."));
        }
    }

    @Test
    public void testExtraRequirementMissingType() throws Exception {
        Xpp3Dom dom = createConfigurationDom("id", "versionRange");
        try {
            configurationReader.readExtraRequirements(new TargetPlatformConfiguration(), dom);
            fail();
        } catch (BuildFailureException e) {
            assertTrue(e.getMessage()
                    .contains("Element <type> is missing in <extraRequirements><requirement> section."));
        }
    }

    @Test
    public void testExtraRequirementId() throws Exception {
        Xpp3Dom dom = createConfigurationDom("type", "versionRange");
        try {
            configurationReader.readExtraRequirements(new TargetPlatformConfiguration(), dom);
            fail();
        } catch (BuildFailureException e) {
            assertTrue(e.getMessage().contains("Element <id> is missing in <extraRequirements><requirement> section."));
        }
    }

    private Xpp3Dom createConfigurationDom(String... requirementChildren) {
        Xpp3Dom dom = new Xpp3Dom("configuration");
        Xpp3Dom extraRequirements = new Xpp3Dom("extraRequirements");
        Xpp3Dom requirement = new Xpp3Dom("requirement");
        extraRequirements.addChild(requirement);
        dom.addChild(extraRequirements);
        for (String requirementChild : requirementChildren) {
            requirement.addChild(new Xpp3Dom(requirementChild));
        }
        return dom;
    }

}
