/*******************************************************************************
 * Copyright (c) 2014, 2020 Obeo and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *     Enrico De Fent - test package inclusion/exclusion (see bug 459214)
 *******************************************************************************/
package org.eclipse.tycho.extras.docbundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.testing.SilentLog;
import org.codehaus.plexus.util.cli.Commandline;
import org.eclipse.tycho.core.osgitools.DefaultBundleReader;
import org.junit.jupiter.api.Test;

public class TestJavadocRunner {

    @Test
    public void testCommandLine() throws Exception {
        JavadocRunner javadocRunner = buildTestRunner();
        JavadocOptions options = new JavadocOptions();
        options.setJvmOptions(Arrays.asList("-Xmx512m"));
        javadocRunner.setOptions(options);

        Commandline commandLine = javadocRunner.createCommandLine("/dev/null");

        String[] cliArgs = commandLine.getArguments();
        assertEquals(2, cliArgs.length);
        assertEquals("@/dev/null", cliArgs[0]);
        assertEquals("-J-Xmx512m", cliArgs[1]);
    }

    @Test
    public void testOptionsFile() throws Exception {
        JavadocRunner javadocRunner = buildTestRunner();
        JavadocOptions options = new JavadocOptions();
        List<Dependency> docletArifacts = new LinkedList<>();
        DocletArtifactsResolver docletResolver = mock(DocletArtifactsResolver.class);
        Set<String> docletArtifactsJarList = new LinkedHashSet<>(
                Arrays.asList("path/to/docletArtifact.jar", "path/to/otherDocletArtifact.jar"));
        when(docletResolver.resolveArtifacts(docletArifacts)).thenReturn(docletArtifactsJarList);
        options.setAdditionalArguments(Arrays.asList("-docencoding \"UTF-8\""));
        options.setDoclet("foo.bar.MyDoclet");
        options.setDocletArtifacts(docletArifacts);
        options.setEncoding("ISO8859_1");
        javadocRunner.setDocletArtifactsResolver(docletResolver);
        javadocRunner.setOptions(options);
        javadocRunner.setSourceFolders(Collections.<File> emptySet());
        javadocRunner.setClassPath(Arrays.asList("rt.jar"));
        javadocRunner.setManifestFiles(Collections.singleton(getTestBundleRoot()));

        String optionsFile = javadocRunner.createOptionsFileContent();
        AssertOnBuffer aob = new AssertOnBuffer(optionsFile);
        aob.assertNextLine("-classpath 'rt.jar'");
        aob.assertNextLine("-doclet foo.bar.MyDoclet");
        aob.assertNextLine(
                "-docletpath 'path/to/docletArtifact.jar" + File.pathSeparator + "path/to/otherDocletArtifact.jar'");
        aob.assertNextLine("-encoding ISO8859_1");
        aob.assertNextLine("-docencoding \"UTF-8\"");
        aob.assertNextLine("com.example.bundle.core");
        aob.assertNextLine("com.example.bundle.core.conf");
        aob.assertNextLine("com.example.bundle.core.internal");
        aob.assertNextLine("com.acme.other.core");
        aob.assertNextLine("com.acme.other.core.internal.utils");
        aob.assertNextLine("com.acme.other.core.internal.ui");
        aob.assertNextLine("nu.xom");
        aob.assertNoMoreLines();
    }

    @Test
    public void testOptionsFileExclude() throws Exception {
        final JavadocRunner javadocRunner = buildTestRunner();
        final JavadocOptions options = new JavadocOptions();

        final List<String> excludes = new LinkedList<>();
        excludes.add("*.internal");
        excludes.add("*.internal.*");
        excludes.add("*.xom");
        options.setExcludes(excludes);

        javadocRunner.setOptions(options);

        final Set<File> manifests = new HashSet<>();
        File file = getTestBundleRoot();
        manifests.add(file);
        javadocRunner.setManifestFiles(manifests);

        String optionsFile = javadocRunner.createOptionsFileContent();
        AssertOnBuffer aob = new AssertOnBuffer(optionsFile);
        aob.assertNextLine("com.example.bundle.core");
        aob.assertNextLine("com.example.bundle.core.conf");
        aob.assertNextLine("com.acme.other.core");
        aob.assertNoMoreLines();
    }

    @Test
    public void testOptionsFileInclude() throws Exception {
        final JavadocRunner javadocRunner = buildTestRunner();
        final JavadocOptions options = new JavadocOptions();

        final List<String> includes = new LinkedList<>();
        includes.add("com.example.*");
        includes.add("nu.xom");
        options.setIncludes(includes);

        javadocRunner.setOptions(options);
        javadocRunner.setManifestFiles(Collections.singleton(getTestBundleRoot()));

        String optionsFile = javadocRunner.createOptionsFileContent();
        AssertOnBuffer aob = new AssertOnBuffer(optionsFile);
        aob.assertNextLine("com.example.bundle.core");
        aob.assertNextLine("com.example.bundle.core.conf");
        aob.assertNextLine("com.example.bundle.core.internal");
        aob.assertNextLine("nu.xom");
        aob.assertNoMoreLines();
    }

    private static final String BUNDLE_ROOT = "bundle";

    /**
     * Returns the local path of the test bundle.
     */
    private File getTestBundleRoot() throws Exception {
        return new File(getClass().getResource(BUNDLE_ROOT).toURI());
    }

    /**
     * Returns a minimal JavadocRunner.
     */
    private JavadocRunner buildTestRunner() {
        final JavadocRunner javadocRunner = new JavadocRunner();
        javadocRunner.setDocletArtifactsResolver(mock(DocletArtifactsResolver.class));
        javadocRunner.setLog(new SilentLog());
        javadocRunner.setBundleReader(new DefaultBundleReader());
        return javadocRunner;
    }
}
