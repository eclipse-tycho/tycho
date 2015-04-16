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
package org.eclipse.tycho.buildnumber.test;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.SilentLog;
import org.eclipse.tycho.buildversion.ValidateVersionMojo;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;
import org.junit.Assert;
import org.junit.Test;

public class ValidateVersionTest extends AbstractTychoMojoTestCase {

    private ValidateVersionMojo mojo;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mojo = new ValidateVersionMojo();
        mojo.setLog(new SilentLog());
    }

    @Test
    public void testValidateVersionWithVersionMatches() throws MojoExecutionException {
        mojo.validateReleaseVersion("1.2.3", "1.2.3");
        mojo.validateSnapshotVersion("1.2.3-SNAPSHOT", "1.2.3.qualifier");
    }

    @Test
    public void testValidateSnapshotVersionWithInvalidVersions() {
        assertInvalidSnapshotVersion("1.2.3-SNAPSHOT", "1.2.3",
                "OSGi version 1.2.3 must have .qualifier qualifier for SNAPSHOT builds");

        assertInvalidSnapshotVersion("1.2.3", "1.2.3.qualifier",
                "Maven version 1.2.3 must have -SNAPSHOT qualifier for SNAPSHOT builds");

        assertInvalidSnapshotVersion("1.2.3-SNAPSHOT", "1.2.0.qualifier",
                "Unqualified OSGi version 1.2.0.qualifier must match unqualified Maven version 1.2.3-SNAPSHOT for SNAPSHOT builds");

        assertInvalidSnapshotVersion("1.2.3.qualifier", "1.2.3.qualifier",
                "Maven version 1.2.3.qualifier must have -SNAPSHOT qualifier for SNAPSHOT builds");

        assertInvalidSnapshotVersion("1.2.3-SNAPSHOT", "1.2.3.SHAPSHOT",
                "OSGi version 1.2.3.SHAPSHOT must have .qualifier qualifier for SNAPSHOT builds");
    }

    private void assertInvalidSnapshotVersion(String maven, String osgi, String expectedMessage) {
        try {
            mojo.validateSnapshotVersion(maven, osgi);
            fail();
        } catch (MojoExecutionException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }
}
