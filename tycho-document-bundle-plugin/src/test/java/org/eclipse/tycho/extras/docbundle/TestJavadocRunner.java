/*******************************************************************************
 * Copyright (c) 2014, 2015 Obeo and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.docbundle;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Dependency;
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
        List<Dependency> docletArifacts = new LinkedList<Dependency>();
        DocletArtifactsResolver docletResolver = mock(DocletArtifactsResolver.class);
        Set<String> docletArtifactsJarList = new LinkedHashSet<String>(Arrays.asList("path/to/docletArtifact.jar",
                "path/to/otherDocletArtifact.jar"));
        when(docletResolver.resolveArtifacts(docletArifacts)).thenReturn(docletArtifactsJarList);
        options.setAdditionalArguments(Arrays.asList("-docencoding \"UTF-8\""));
        options.setJvmOptions(Arrays.asList("-Xmx512m"));
        options.setDoclet("foo.bar.MyDoclet");
        options.setDocletArtifacts(docletArifacts);
        options.setEncoding("ISO8859_1");
        javadocRunner.setDocletArtifactsResolver(docletResolver);
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
        String lineSeparator = System.getProperty("line.separator");
        String expectedOptionsFile = "-classpath 'rt.jar'" + lineSeparator 
                + "-doclet foo.bar.MyDoclet" + lineSeparator
                + "-docletpath 'path/to/docletArtifact.jar" + File.pathSeparator + "path/to/otherDocletArtifact.jar'" + lineSeparator
                + "-encoding ISO8859_1" + lineSeparator
                + "-docencoding \"UTF-8\"" + lineSeparator;
        Assert.assertEquals(expectedOptionsFile, optionsFile);
    }
}
