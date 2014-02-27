/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.surefire;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import junit.framework.TestCase;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.sisu.equinox.launching.DefaultEquinoxInstallationDescription;
import org.eclipse.sisu.equinox.launching.internal.DefaultEquinoxInstallation;
import org.eclipse.sisu.equinox.launching.internal.EquinoxLaunchConfiguration;

public class TestMojoTest extends TestCase {

    public void testSplitArgLineNull() throws Exception {
        TestMojo testMojo = new TestMojo();
        String[] parts = testMojo.splitArgLine(null);
        assertNotNull(parts);
        assertEquals(0, parts.length);
    }

    public void testSplitArgLineMultipleArgs() throws Exception {
        TestMojo testMojo = new TestMojo();
        String[] parts = testMojo.splitArgLine(" -Dfoo=bar -Dkey2=value2 \"-Dkey3=spacy value\"");
        assertEquals(3, parts.length);
        assertEquals("-Dfoo=bar", parts[0]);
        assertEquals("-Dkey2=value2", parts[1]);
        assertEquals("-Dkey3=spacy value", parts[2]);
    }

    public void testSplitArgLineUnbalancedQuotes() throws Exception {
        TestMojo testMojo = new TestMojo();
        try {
            testMojo.splitArgLine("\"'missing closing double-quote'");
            fail("unreachable code");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), containsString("unbalanced quotes"));
        }
    }

    public void testAddProgramArgsWithSpaces() throws Exception {
        EquinoxLaunchConfiguration cli = createEquinoxConfiguration();
        TestMojo testMojo = new TestMojo();
        testMojo.addProgramArgs(cli, "-data", "/path with spaces ");
        assertEquals(2, cli.getProgramArguments().length);
        assertEquals("-data", cli.getProgramArguments()[0]);
        assertEquals("/path with spaces ", cli.getProgramArguments()[1]);
    }

    public void testAddProgramArgsNullArg() throws Exception {
        EquinoxLaunchConfiguration cli = createEquinoxConfiguration();
        TestMojo testMojo = new TestMojo();
        // null arg must be ignored
        testMojo.addProgramArgs(cli, "-data", null);
        assertEquals(1, cli.getProgramArguments().length);
    }

    private EquinoxLaunchConfiguration createEquinoxConfiguration() {
        DefaultEquinoxInstallation testRuntime = new DefaultEquinoxInstallation(
                new DefaultEquinoxInstallationDescription(), null, null);
        return new EquinoxLaunchConfiguration(testRuntime);
    }

}
