/*******************************************************************************
 * Copyright (c) 2014 Obeo and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.docbundle;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import org.apache.maven.plugin.testing.SilentLog;
import org.codehaus.plexus.util.cli.Commandline;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author <a href="mailto:mikael.barbero@obeo.fr">Mikael Barbero</a>
 */
public class TestJavadocRunner {

    @Test
    public void testCommandLine() throws Exception {
        JavadocRunner javadocRunner = new JavadocRunner();
        JavadocOptions options = new JavadocOptions();
        options.setAdditionalArguments(Arrays.asList("-docencoding \"UTF-8\""));
        options.setJvmOptions(Arrays.asList("-Xmx512m"));
        javadocRunner.setLog(new SilentLog());
        javadocRunner.setOptions(options);
        javadocRunner.setSourceFolders(Collections.<File> emptySet());
        javadocRunner.setClassPath(Arrays.asList("rt.jar"));
        javadocRunner.setManifestFiles(Collections.<File> emptySet());
        Commandline commandLine = javadocRunner.createCommandLine("/dev/null");
        String[] cliArgs = commandLine.getArguments();
        Assert.assertEquals(2, cliArgs.length);
        Assert.assertEquals("@/dev/null", cliArgs[0]);
        Assert.assertEquals("-J-Xmx512m", cliArgs[1]);

        String optionsFile = javadocRunner.createOptionsFileContent();
        Assert.assertEquals("-classpath 'rt.jar'" + System.getProperty("line.separator") + "-docencoding \"UTF-8\""
                + System.getProperty("line.separator"), optionsFile);
    }
}
