/*******************************************************************************
 * Copyright (c) 2008, 2015 Sonatype Inc. and others.
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
package org.eclipse.tycho.buildnumber.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.tycho.buildversion.ValidateVersionMojo;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;
import org.junit.Assert;
import org.junit.Test;

public class ValidateVersionTest extends AbstractTychoMojoTestCase {

    private ValidateVersionMojo mojo;
    private Log log;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mojo = new ValidateVersionMojo();
        log = mock(Log.class);
        mojo.setLog(log);
    }

    @Test
    public void testValidateVersionWithVersionMatches() throws MojoExecutionException {
        mojo.validateReleaseVersion("1.2.3", "1.2.3");
        mojo.validateSnapshotVersion("1.2.3-SNAPSHOT", "1.2.3.qualifier");
    }

    @Test
    public void testValidateSnapshotVersionWithInvalidVersionsUsingStrictVersions()
            throws MojoExecutionException, IllegalAccessException {
        testValidateSnapshotVersionWithInvalidVersions(true);
    }

    @Test
    public void testValidateSnapshotVersionWithInvalidVersionsUsingNonStrictVersions()
            throws MojoExecutionException, IllegalAccessException {
        testValidateSnapshotVersionWithInvalidVersions(false);
    }

    private void testValidateSnapshotVersionWithInvalidVersions(Boolean strictVersions)
            throws MojoExecutionException, IllegalAccessException {
        setVariableValueToObject(mojo, "strictVersions", strictVersions);
        assertInvalidSnapshotVersion("1.2.3-SNAPSHOT", "1.2.3",
                "OSGi version 1.2.3 must have .qualifier qualifier for SNAPSHOT builds", strictVersions);

        assertInvalidSnapshotVersion("1.2.3", "1.2.3.qualifier",
                "Maven version 1.2.3 must have -SNAPSHOT qualifier for SNAPSHOT builds", strictVersions);

        assertInvalidSnapshotVersion("1.2.3-SNAPSHOT", "1.2.0.qualifier",
                "Unqualified OSGi version 1.2.0.qualifier must match unqualified Maven version 1.2.3-SNAPSHOT for SNAPSHOT builds",
                strictVersions);

        assertInvalidSnapshotVersion("1.2.3.qualifier", "1.2.3.qualifier",
                "Maven version 1.2.3.qualifier must have -SNAPSHOT qualifier for SNAPSHOT builds", strictVersions);

        assertInvalidSnapshotVersion("1.2.3-SNAPSHOT", "1.2.3.SNAPSHOT",
                "OSGi version 1.2.3.SNAPSHOT must have .qualifier qualifier for SNAPSHOT builds", strictVersions);
    }

    private void assertInvalidSnapshotVersion(String maven, String osgi, String expectedMessage, boolean strictVersions)
            throws MojoExecutionException {
        try {
            mojo.validateSnapshotVersion(maven, osgi);
            if (strictVersions) {
                fail();
            } else {
                verify(log).warn(expectedMessage);
            }
        } catch (MojoExecutionException e) {
            if (strictVersions) {
                Assert.assertEquals(expectedMessage, e.getMessage());
            } else {
                throw e;
            }
        }
    }
}
