/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.symlinks;

import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.it.Verifier;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.BeforeClass;
import org.junit.Test;

public class SymbolicLinkTest extends AbstractTychoIntegrationTest {

    private static final String BASEDIR = "/symLink";
    private static final String LINK_NAME = "linkToProject";

    private static Verifier verifier;

    @BeforeClass
    public static void checkLinkCreation() throws Exception {
        SymbolicLinkTest instance = new SymbolicLinkTest();

        verifier = instance.getVerifier(BASEDIR, false);
        // Note that calling getBaseDir() will trigger copying the test project
        File projectDir = new File(verifier.getBasedir(), "project");
        File linkToProjectDir = new File(projectDir.getParentFile(), LINK_NAME);

        // if these checks fail, the test will be marked as skipped by surefire
        assumeTrue(createSymbolicLink(projectDir, linkToProjectDir));
        assumeTrue(new File(linkToProjectDir, "pom.xml").isFile());
    }

    @Test
    public void testBuildWithSymbolicLinkOnProjectPath() throws Exception {
        verifier.getCliOptions().addAll(Arrays.asList("-f", LINK_NAME + "/pom.xml"));
        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();
    }

    private static boolean createSymbolicLink(File target, File link) {
        Commandline commandline = new Commandline();
        commandline.setWorkingDirectory(link.getParentFile());
        commandline.addArguments(new String[] { "ln", "-s", target.getAbsolutePath(), link.getAbsolutePath() });
        try {
            int result = CommandLineUtils.executeCommandLine(commandline, new DefaultConsumer(), new DefaultConsumer());
            return 0 == result && link.exists();
        } catch (Throwable e) {
            return false;
        }
    }

}
