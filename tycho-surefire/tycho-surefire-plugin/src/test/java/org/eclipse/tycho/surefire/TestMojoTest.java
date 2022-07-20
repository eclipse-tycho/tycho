/*******************************************************************************
 * Copyright (c) 2010, 2017 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *     Bachmann electrontic GmbH - Adding tests for the parallel mode
 *******************************************************************************/
package org.eclipse.tycho.surefire;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.surefire.api.booter.ProviderParameterNames;
import org.apache.maven.surefire.api.util.ScanResult;
import org.codehaus.plexus.util.ReflectionUtils;
import org.eclipse.sisu.equinox.launching.internal.DefaultEquinoxInstallation;
import org.eclipse.sisu.equinox.launching.internal.DefaultEquinoxInstallationDescription;
import org.eclipse.sisu.equinox.launching.internal.EquinoxLaunchConfiguration;
import org.junit.Test;

public class TestMojoTest {

    @Test
    public void testSplitArgLineNull() throws Exception {
        AbstractTestMojo testMojo = new TestPluginMojo();
        String[] parts = testMojo.splitArgLine(null);
        assertNotNull(parts);
        assertEquals(0, parts.length);
    }

    @Test
    public void testSplitArgLineMultipleArgs() throws Exception {
        AbstractTestMojo testMojo = new TestPluginMojo();
        String[] parts = testMojo.splitArgLine(" -Dfoo=bar -Dkey2=value2 \"-Dkey3=spacy value\"");
        assertEquals(3, parts.length);
        assertEquals("-Dfoo=bar", parts[0]);
        assertEquals("-Dkey2=value2", parts[1]);
        assertEquals("-Dkey3=spacy value", parts[2]);
    }

    @Test
    public void testSplitArgLineUnbalancedQuotes() throws Exception {
        AbstractTestMojo testMojo = new TestPluginMojo();
        try {
            testMojo.splitArgLine("\"'missing closing double-quote'");
            fail("unreachable code");
        } catch (MojoExecutionException e) {
            assertTrue(e.getMessage().contains("unbalanced quotes"));
        }
    }

    @Test
    public void testAddProgramArgsWithSpaces() throws Exception {
        EquinoxLaunchConfiguration cli = createEquinoxConfiguration();
        AbstractTestMojo testMojo = new TestPluginMojo();
        testMojo.addProgramArgs(cli, "-data", "/path with spaces ");
        assertEquals(2, cli.getProgramArguments().length);
        assertEquals("-data", cli.getProgramArguments()[0]);
        assertEquals("/path with spaces ", cli.getProgramArguments()[1]);
    }

    @Test
    public void testAddProgramArgsNullArg() throws Exception {
        EquinoxLaunchConfiguration cli = createEquinoxConfiguration();
        AbstractTestMojo testMojo = new TestPluginMojo();
        // null arg must be ignored
        testMojo.addProgramArgs(cli, "-data", null);
        assertEquals(1, cli.getProgramArguments().length);
    }

    @Test
    public void testShouldSkipWithNoValueSet() {
        AbstractTestMojo testMojo = new TestPluginMojo();
        assertFalse(testMojo.shouldSkip());
    }

    @Test
    public void testShouldSkipWithSkipTestsSetToTrue() throws Exception {
        AbstractTestMojo testMojo = new TestPluginMojo();
        setParameter(testMojo, "skipTests", Boolean.TRUE);
        assertTrue(testMojo.shouldSkip());
    }

    @Test
    public void testShouldSkipWithMavenTestSkipSetToTrue() throws Exception {
        AbstractTestMojo testMojo = new TestPluginMojo();
        setParameter(testMojo, "skip", Boolean.TRUE);
        assertTrue(testMojo.shouldSkip());
    }

    @Test
    public void testShouldSkipThatSkipTestsWillBePrefered() throws Exception {
        AbstractTestMojo testMojo = new TestPluginMojo();
        setParameter(testMojo, "skip", Boolean.FALSE);
        setParameter(testMojo, "skipTests", Boolean.TRUE);
        assertTrue(testMojo.shouldSkip());
    }

    @Test
    public void testShouldSkipWithSkipExeSetToTrue() throws Exception {
        AbstractTestMojo testMojo = new TestPluginMojo();
        setParameter(testMojo, "skipExec", Boolean.TRUE);
        assertTrue(testMojo.shouldSkip());
    }

    @Test
    public void testExcludes() throws Exception {
        List<String> includes = new ArrayList<>();
        includes.add("*");
        List<String> excludes = new ArrayList<>();
        // adding a null to simulate an unresolved parameter interpolation
        excludes.add(null);
        excludes.add("*Another*");
        ScanResult result = createDirectoryAndScanForTests(includes, excludes);
        assertEquals(1, result.size());
    }

    @Test
    public void testIncludes() throws Exception {
        List<String> includes = new ArrayList<>();
        includes.add("*Another*");
        includes.add(null);
        ScanResult result = createDirectoryAndScanForTests(includes, null);
        assertEquals(1, result.size());
    }

    @Test
    public void testParallelModeMissingThreadCountParameter() throws Exception {
        AbstractTestMojo testMojo = new TestPluginMojo();
        setParameter(testMojo, "parallel", ParallelMode.both);
        try {
            testMojo.getMergedProviderProperties();
            fail("MojoExecutionException expected since threadCount parameter is missing");
        } catch (MojoExecutionException e) {
            // Success
        }
    }

    @Test
    public void testParallelModeThreadCountSetTo1() throws Exception {
        AbstractTestMojo testMojo = new TestPluginMojo();
        setParameter(testMojo, "parallel", ParallelMode.both);
        setParameter(testMojo, "threadCount", 1);
        try {
            testMojo.getMergedProviderProperties();
            fail("MojoExecutionException expected since threadCount parameter is missing");
        } catch (MojoExecutionException e) {
            // Success
        }
    }

    @Test
    public void testParallelModeWithThreadCountSet() throws Exception {
        AbstractTestMojo testMojo = new TestPluginMojo();
        setParameter(testMojo, "parallel", ParallelMode.both);
        setParameter(testMojo, "threadCount", 2);
        Properties providerProperties = testMojo.getMergedProviderProperties();
        assertEquals("both", providerProperties.get(ProviderParameterNames.PARALLEL_PROP));
        assertEquals("2", providerProperties.get(ProviderParameterNames.THREADCOUNT_PROP));
    }

    @Test
    public void testParallelModeWithUseUnlimitedThreads() throws Exception {
        AbstractTestMojo testMojo = new TestPluginMojo();
        setParameter(testMojo, "parallel", ParallelMode.both);
        setParameter(testMojo, "useUnlimitedThreads", Boolean.TRUE);
        Properties providerProperties = testMojo.getMergedProviderProperties();
        assertEquals("both", providerProperties.get(ProviderParameterNames.PARALLEL_PROP));
        assertEquals("true", providerProperties.get("useUnlimitedThreads"));
    }

    @Test(expected = MojoExecutionException.class)
    public void testParallelModeWithPerCoreThreadCountMissingCount() throws Exception {
        AbstractTestMojo testMojo = new TestPluginMojo();
        setParameter(testMojo, "parallel", ParallelMode.both);
        setParameter(testMojo, "perCoreThreadCount", true);
        testMojo.getMergedProviderProperties();
    }

    @Test
    public void testParallelModeWithPerCoreThreadCount() throws Exception {
        AbstractTestMojo testMojo = new TestPluginMojo();
        setParameter(testMojo, "parallel", ParallelMode.both);
        setParameter(testMojo, "perCoreThreadCount", true);
        setParameter(testMojo, "threadCount", 1);
        Properties providerProperties = testMojo.getMergedProviderProperties();
        assertEquals("both", providerProperties.get(ProviderParameterNames.PARALLEL_PROP));
        assertEquals("1", providerProperties.get(ProviderParameterNames.THREADCOUNT_PROP));
        assertEquals("true", providerProperties.get("perCoreThreadCount"));
    }

    public ScanResult createDirectoryAndScanForTests(List<String> includes, List<String> excludes) throws Exception {
        File directory = null;
        try {
            AbstractTestMojo testMojo = new TestPluginMojo();
            directory = Files.createTempDirectory(this.getClass().getName()).toFile();
            File aTestFile = new File(directory, "ATest.class");
            aTestFile.createNewFile();
            File anotherTestcase = new File(directory, "AnotherTestCase.class");
            anotherTestcase.createNewFile();

            setParameter(testMojo, "includes", includes);
            setParameter(testMojo, "excludes", excludes);
            setParameter(testMojo, "testClassesDirectory", directory);
            return testMojo.scanForTests();
        } finally {
            if (directory != null) {
                directory.delete();
            }
        }
    }

    private void setParameter(Object object, String variable, Object value)
            throws IllegalArgumentException, IllegalAccessException {
        Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses(variable, object.getClass());
        field.setAccessible(true);
        field.set(object, value);
    }

    private EquinoxLaunchConfiguration createEquinoxConfiguration() {
        DefaultEquinoxInstallation testRuntime = new DefaultEquinoxInstallation(
                new DefaultEquinoxInstallationDescription(), null, null);
        return new EquinoxLaunchConfiguration(testRuntime);
    }

}
