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

import junit.framework.TestCase;

import org.eclipse.sisu.equinox.launching.DefaultEquinoxInstallationDescription;
import org.eclipse.sisu.equinox.launching.internal.DefaultEquinoxInstallation;
import org.eclipse.sisu.equinox.launching.internal.EquinoxLaunchConfiguration;

public class TestMojoTest extends TestCase {

    public void testVMArgLineMultipleArgs() throws Exception {
        EquinoxLaunchConfiguration cli = createEquinoxConfiguration();
        TestMojo testMojo = new TestMojo();
        testMojo.addVMArgs(cli, " -Dfoo=bar -Dkey2=value2 ");
        String[] vmArguments = cli.getVMArguments();
        assertEquals(1, vmArguments.length);
        assertEquals(" -Dfoo=bar -Dkey2=value2 ", vmArguments[0]);
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
