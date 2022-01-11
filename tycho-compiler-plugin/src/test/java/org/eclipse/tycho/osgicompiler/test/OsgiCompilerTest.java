/*******************************************************************************
 * Copyright (c) 2008, 2018 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgicompiler.test;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.classpath.SourcepathEntry;
import org.eclipse.tycho.compiler.AbstractOsgiCompilerMojo;
import org.eclipse.tycho.core.ee.StandardExecutionEnvironment;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;

import copied.org.apache.maven.plugin.CompilationFailureException;

public class OsgiCompilerTest extends AbstractTychoMojoTestCase {

    private static final int TARGET_1_4 = 48;
    private static final int TARGET_1_8 = 52;

    protected File storage;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        storage = new File(getBasedir(), "target/storage");
        FileUtils.deleteDirectory(storage);
    }

    private AbstractOsgiCompilerMojo getMojo(List<MavenProject> projects, MavenProject project) throws Exception {
        AbstractOsgiCompilerMojo mojo = (AbstractOsgiCompilerMojo) lookupConfiguredMojo(project, "compile");
//        setVariableValueToObject(mojo, "project", project);
//        setVariableValueToObject(mojo, "session", newMavenSession(project, projects));

        // tycho-compiler-jdt does not support forked compilation
//		        setVariableValueToObject(mojo, "fork", fork? Boolean.TRUE: Boolean.FALSE);
        return mojo;
    }

    public void testAccessRestrictionCompilationError() throws Exception {
        File basedir = getBasedir("projects/accessrules");
        List<MavenProject> projects = getSortedProjects(basedir);

        try {
            for (MavenProject project : projects) {
                if (!"pom".equals(project.getPackaging())) {
                    getMojo(projects, project).execute();
                }
            }
            fail("Restricted package access");
        } catch (MojoFailureException e) {
            assertTrue(e.getLongMessage().contains("The type 'P001Impl' is not API"));
        }
    }

    public void testAccessRulesClasspath() throws Exception {
        File basedir = getBasedir("projects/accessrules");
        List<MavenProject> projects = getSortedProjects(basedir);

        getMojo(projects, projects.get(1)).execute();
        getMojo(projects, projects.get(2)).execute();
        getMojo(projects, projects.get(3)).execute();

        MavenProject project = projects.get(4);
        AbstractOsgiCompilerMojo mojo = getMojo(projects, project);
        List<String> cp = mojo.getClasspathElements();
        assertEquals(4, cp.size());
        assertEquals(getClasspathElement(new File(getBasedir()), "target/projects/accessrules/p001/target/classes",
                "[+p001/*:?**/*]"), cp.get(0));
        // note that PDE sorts dependencies coming via imported-package by symbolicName_version
        assertEquals(getClasspathElement(new File(getBasedir()), "target/projects/accessrules/p003/target/classes",
                "[+p003/*:?**/*]"), cp.get(1));
        assertEquals(getClasspathElement(new File(getBasedir()), "target/projects/accessrules/p004/target/classes",
                "[+p004/*:?**/*]"), cp.get(2));
        assertEquals(getClasspathElement(project.getBasedir(), "target/classes", ""), cp.get(3));

    }

    public void testClasspath() throws Exception {
        File basedir = getBasedir("projects/classpath");
        List<MavenProject> projects = getSortedProjects(basedir,
                new File(getBasedir(), "src/test/resources/projects/classpath/platform"));

        MavenProject project;
        List<String> cp;

        // simple project
        project = projects.get(1);
        AbstractOsgiCompilerMojo mojo = getMojo(projects, project);
        cp = mojo.getClasspathElements();
        assertEquals(1, cp.size());
        assertEquals(getClasspathElement(project.getBasedir(), "target/classes", ""), cp.get(0));

        // project with nested lib
        project = projects.get(2);
        mojo = getMojo(projects, project);
        cp = mojo.getClasspathElements();
        assertEquals(2, cp.size());
        assertEquals(getClasspathElement(project.getBasedir(), "target/classes", ""), cp.get(0));
        assertEquals(getClasspathElement(project.getBasedir(), "lib/lib.jar", ""), cp.get(1));

        // project with external dependency with nested jar
        project = projects.get(3);
        mojo = getMojo(projects, project);
        cp = mojo.getClasspathElements();
        assertEquals(3, cp.size());
        final String plainJarPath = "src/test/resources/projects/classpath/platform/plugins/p003_0.0.1.jar";
        final String nestedJarPath = "target/local-repo/.cache/tycho/p003_0.0.1.jar/lib/lib.jar";
        assertEquals(getClasspathElement(new File(getBasedir()), plainJarPath, "[?**/*]"), cp.get(0));
        assertEquals(getClasspathElement(new File(getBasedir()), nestedJarPath, "[?**/*]"), cp.get(1));
        assertEquals(getClasspathElement(project.getBasedir(), "target/classes", ""), cp.get(2));

        // project with a (not yet) existing nested jar that would be copied later during build 
        // (wrapper scenario with copy-pom-dependencies)
        project = projects.get(4);
        mojo = getMojo(projects, project);
        mojo.execute();
        cp = mojo.getClasspathElements();
        assertEquals(3, cp.size());
        assertEquals(getClasspathElement(project.getBasedir(), "target/classes", ""), cp.get(0));
        assertEquals(getClasspathElement(project.getBasedir(), "lib/not_existing_yet.jar", ""), cp.get(1));
        assertEquals(getClasspathElement(project.getBasedir(), "lib/not_existing_yet_dir/", ""), cp.get(2));
    }

    private String getClasspathElement(File base, String path, String accessRules) throws IOException {
        String file = new File(base, path).getAbsolutePath();
        return file + accessRules.replace(":", AbstractOsgiCompilerMojo.RULE_SEPARATOR);
    }

    public void test_multisourceP001_viaMojoConfiguration() throws Exception {
        File basedir = getBasedir("projects/multisource/p001");
        List<MavenProject> projects = getSortedProjects(basedir);

        MavenProject project = projects.get(0);
        getMojo(projects, project).execute();

        assertTrue(new File(project.getBasedir(), "target/classes/p001/p1/P1.class").canRead());
        assertTrue(new File(project.getBasedir(), "target/classes/p001/p2/P2.class").canRead());
    }

    public void test_multisourceP002_viaBuildProperties() throws Exception {
        File basedir = getBasedir("projects/multisource/p002");
        List<MavenProject> projects = getSortedProjects(basedir);

        MavenProject project = projects.get(0);
        getMojo(projects, project).execute();

        assertTrue(new File(project.getBasedir(), "target/classes/p002/p1/P1.class").canRead());
        assertTrue(new File(project.getBasedir(), "target/classes/p002/p2/P2.class").canRead());
    }

    public void test_multipleOutputJars() throws Exception {
        File basedir = getBasedir("projects/multijar");
        List<MavenProject> projects = getSortedProjects(basedir);

        MavenProject project = projects.get(0);
        getMojo(projects, project).execute();

        assertTrue(new File(project.getBasedir(), "target/classes/src/Src.class").canRead());
        assertTrue(new File(project.getBasedir(), "target/library.jar-classes/src2/Src2.class").canRead());

        List<SourcepathEntry> sourcepath = getMojo(projects, project).getSourcepath();
        assertEquals(2, sourcepath.size());
    }

    public void test_multipleOutputJars_getSourcepath() throws Exception {
        File basedir = getBasedir("projects/multijar");
        List<MavenProject> projects = getSortedProjects(basedir);

        MavenProject project = projects.get(0);

        List<SourcepathEntry> sourcepath = getMojo(projects, project).getSourcepath();
        assertEquals(2, sourcepath.size());
        assertSameFile(new File(project.getBasedir(), "target/classes"), sourcepath.get(0).getOutputDirectory());
        assertSameFile(new File(project.getBasedir(), "src"), sourcepath.get(0).getSourcesRoot());
        assertSameFile(new File(project.getBasedir(), "target/library.jar-classes"),
                sourcepath.get(1).getOutputDirectory());
        assertSameFile(new File(project.getBasedir(), "src2"), sourcepath.get(1).getSourcesRoot());
    }

    private void assertSameFile(File expected, File actual) throws IOException {
        assertEquals(expected.getCanonicalFile(), actual.getCanonicalFile());
    }

    public void testCopyResources() throws Exception {
        File basedir = getBasedir("projects/resources/p001");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project = projects.get(0);
        getMojo(projects, project).execute();
        assertTrue(new File(project.getBasedir(), "target/classes/testresources/Test.class").canRead());
        assertTrue(new File(project.getBasedir(), "target/classes/testresources/test.properties").canRead());
    }

    public void testCopyResourcesWithNestedJar() throws Exception {
        File basedir = getBasedir("projects/resources/p004");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project = projects.get(0);
        getMojo(projects, project).execute();
        assertTrue(new File(project.getBasedir(), "target/classes/testresources/Test.class").canRead());
        assertTrue(new File(project.getBasedir(), "target/classes/testresources/test.properties").canRead());
    }

    public void testExcludeCopyResources() throws Exception {
        File basedir = getBasedir("projects/resources/p002");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project = projects.get(0);
        getMojo(projects, project).execute();
        assertTrue(new File(project.getBasedir(), "target/classes/testresources/Test.class").canRead());
        assertFalse(new File(project.getBasedir(), "target/classes/testresources/Test.aj").canRead());
    }

    public void testCopyResourcesWithResourceCopyingSetToOff() throws Exception {
        File basedir = getBasedir("projects/resources/p003");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project = projects.get(0);
        getMojo(projects, project).execute();
        assertTrue(new File(project.getBasedir(), "target/classes/testresources/Test.class").exists());
        assertFalse(new File(project.getBasedir(), "target/classes/testresources/test.properties").exists());
    }

    public void testSourceCompileLevel() throws Exception {
        File basedir = getBasedir("projects/executionEnvironment");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project;
        // project with neither POM nor MANIFEST configuration => must fallback to 
        // source/target level == 11
        project = projects.get(1);
        getMojo(projects, project).execute();
        assertBytecodeMajorLevel(55 /* Java 11 */, new File(project.getBasedir(), "target/classes/Generic.class"));

        // project with multiple execution envs.
        // Minimum source and target level must be taken
        project = projects.get(2);
        AbstractOsgiCompilerMojo mojo = getMojo(projects, project);
        assertEquals("1.3", mojo.getSourceLevel());
        try {
            mojo.execute();
            fail("compilation failure due to assert keyword expected");
        } catch (CompilationFailureException e) {
            // expected
        }
        // project with both explicit compiler configuration in pom.xml and Bundle-RequiredExecutionEnvironment. 
        // explicit compiler configuration in the pom should win. see https://issues.sonatype.org/browse/TYCHO-476
        project = projects.get(3);
        mojo = getMojo(projects, project);
        assertEquals("jsr14", mojo.getTargetLevel());
        assertEquals("1.5", mojo.getSourceLevel());
        mojo.execute();
        assertBytecodeMajorLevel(TARGET_1_4, new File(project.getBasedir(), "target/classes/Generic.class"));
        // project with both explicit EE configuration in pom.xml and Bundle-RequiredExecutionEnvironment.
        // explicit configuration in the pom.xml win
        project = projects.get(4);
        mojo = getMojo(projects, project);
        mojo.execute();
        assertEquals("1.3", mojo.getSourceLevel());
        // project with both explicit compiler configuration in build.properties and Bundle-RequiredExecutionEnvironment. 
        // build.properties should win. 
        project = projects.get(5);
        mojo = getMojo(projects, project);
        mojo.execute();
        assertEquals("jsr14", mojo.getTargetLevel());
        assertEquals("1.5", mojo.getSourceLevel());
        assertBytecodeMajorLevel(TARGET_1_4, new File(project.getBasedir(), "target/classes/Generic.class"));
    }

    public void testNewerEEthanBREE() throws Exception {
        File basedir = getBasedir("projects/executionEnvironment/p006-newerEEthanBREE");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project = projects.get(0);
        AbstractOsgiCompilerMojo mojo = getMojo(projects, project);
        mojo.execute();
        assertTrue(Integer.parseInt(mojo.getExecutionEnvironment().substring("JavaSE-".length())) >= 11);
        assertEquals("1.8", mojo.getSourceLevel());
        assertEquals("1.8", mojo.getTargetLevel());
        assertEquals("8", mojo.getReleaseLevel());
        assertBytecodeMajorLevel(TARGET_1_8, new File(project.getBasedir(), "target/classes/Noop.class"));
    }

    public void testNoBREEButEERequirement() throws Exception {
        File basedir = getBasedir("projects/executionEnvironment/eeAsRequirement");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project = projects.get(0);
        AbstractOsgiCompilerMojo mojo = getMojo(projects, project);
        StandardExecutionEnvironment[] ees = mojo.getBREE();
        assertEquals(Set.of("JavaSE-1.8", "JavaSE/compact1-1.8"),
                Arrays.stream(ees).map(StandardExecutionEnvironment::getProfileName).collect(Collectors.toSet()));
    }

    public void testAutomaticReleaseCompilerArgumentDeterminationDisabled() throws Exception {
        File basedir = getBasedir(
                "projects/executionEnvironment/p007-automaticReleaseCommpilerArgumentDeterminationDisabled");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project = projects.get(0);
        AbstractOsgiCompilerMojo mojo = getMojo(projects, project);
        mojo.execute();
        assertTrue(Integer.parseInt(mojo.getExecutionEnvironment().substring("JavaSE-".length())) >= 11);
        assertEquals("1.8", mojo.getSourceLevel());
        assertEquals("1.8", mojo.getTargetLevel());
        assertNull(mojo.getReleaseLevel());
        assertBytecodeMajorLevel(TARGET_1_8, new File(project.getBasedir(), "target/classes/Noop.class"));
    }

    private void assertBytecodeMajorLevel(int majorLevel, File classFile) throws ClassFormatException, IOException {
        assertTrue(classFile.canRead());
        JavaClass javaClass = new ClassParser(classFile.getAbsolutePath()).parse();
        assertEquals(majorLevel, javaClass.getMajor());
    }

    public void test_TYCHO0400indirectDependencies() throws Exception {
        File basedir = getBasedir("projects/indirectDependencies");
        List<MavenProject> projects = getSortedProjects(basedir);

        assertEquals("C", projects.get(1).getArtifactId());
        getMojo(projects, projects.get(1)).execute();

        assertEquals("B", projects.get(2).getArtifactId());
        getMojo(projects, projects.get(2)).execute();

        assertEquals("A", projects.get(3).getArtifactId());
        AbstractOsgiCompilerMojo mojo = getMojo(projects, projects.get(3));
        List<String> cp = mojo.getClasspathElements();
        assertEquals(getClasspathElement(projects.get(1).getBasedir(), "target/classes", "[?**/*]"), cp.get(1));

        mojo.execute();
        assertTrue(new File(projects.get(3).getBasedir(), "target/classes/a/A.class").canRead());
    }

    public void test_embeddedNonClasspath() throws Exception {
        File basedir = getBasedir("projects/embedednonclasspath");
        List<MavenProject> projects = getSortedProjects(basedir);

        MavenProject project = projects.get(0);
        getMojo(projects, project).execute();

        assertTrue(new File(project.getBasedir(), "target/classes/src/Src.class").canRead());
        assertTrue(new File(project.getBasedir(), "target/library.jar-classes/src2/Src2.class").canRead());

        List<SourcepathEntry> sourcepath = getMojo(projects, project).getSourcepath();
        assertEquals(2, sourcepath.size());
    }

    public void test_bootclasspathAccessRules() throws Exception {
        File basedir = getBasedir("projects/bootclasspath-accessrules");
        List<MavenProject> projects = getSortedProjects(basedir);

        MavenProject project = projects.get(0);
        getMojo(projects, project).execute();
    }

    public void testWarningAndErrorMessages() throws Exception {
        File basedir = getBasedir("projects/compilermessages");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project = projects.get(0);
        AbstractOsgiCompilerMojo mojo = getMojo(projects, project);
        final List<CharSequence> warnings = new ArrayList<>();
        mojo.setLog(new SystemStreamLog() {

            @Override
            public void warn(CharSequence content) {
                warnings.add(content);
            }

        });
        try {
            mojo.execute();
            fail("compilation failure expected");
        } catch (CompilationFailureException e) {
            String message = e.getLongMessage();
            assertThat(message, containsString("3 problems (1 error, 2 warnings)"));
            // 1 error
            assertThat(message, containsString("Test.java:[23"));
            assertThat(message, containsString("System.foo();"));
        }
        // 2 warnings
        List<String> expectedWarnings = asList("Test.java:[19", //
                "Test.java:[21");
        assertEquals(expectedWarnings.size(), warnings.size());
        for (int i = 0; i < warnings.size(); i++) {
            String warning = (String) warnings.get(i);
            String expectedWarning = expectedWarnings.get(i);
            assertThat(warning, containsString(expectedWarning));
        }
    }

    public void testCompilerArgs() throws Exception {
        File basedir = getBasedir("projects/compiler-args");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project = projects.get(0);
        AbstractOsgiCompilerMojo mojo = getMojo(projects, project);

        setVariableValueToObject(mojo, "showWarnings", Boolean.TRUE);

        final List<CharSequence> warnings = new ArrayList<>();
        mojo.setLog(new SystemStreamLog() {

            @Override
            public void warn(CharSequence content) {
                warnings.add(content);
            }

        });
        try {
            mojo.execute();
            fail("compilation failure expected");
        } catch (CompilationFailureException e) {
            String message = e.getLongMessage();
            assertThat(message, containsString("2 problems (1 error, 1 warning)"));
            // 1 error
            assertThat(message, containsString("unused"));
        }
        // 1 warning
        assertThat((String) warnings.iterator().next(), containsString("is boxed"));
    }

    public void testUseProjectSettingsSetToTrue() throws Exception {
        // the code in the project does use boxing and the settings file 
        // turns on warning for auto boxing so we expect here a warning
        File basedir = getBasedir("projects/projectSettings/p001");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project = projects.get(0);
        AbstractOsgiCompilerMojo mojo = getMojo(projects, project);
        setVariableValueToObject(mojo, "useProjectSettings", Boolean.TRUE);
        setVariableValueToObject(mojo, "showWarnings", Boolean.TRUE);
        final List<CharSequence> warnings = new ArrayList<>();
        mojo.setLog(new SystemStreamLog() {

            @Override
            public void warn(CharSequence content) {
                warnings.add(content);
            }

        });
        mojo.execute();
        assertThat((String) warnings.iterator().next(), containsString("is boxed"));
    }

    public void testUseProjectSettingsSetToFalse() throws Exception {
        File basedir = getBasedir("projects/projectSettings/p001");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project = projects.get(0);
        AbstractOsgiCompilerMojo mojo = getMojo(projects, project);
        setVariableValueToObject(mojo, "useProjectSettings", Boolean.FALSE);
        final List<CharSequence> warnings = new ArrayList<>();
        mojo.setLog(new SystemStreamLog() {

            @Override
            public void warn(CharSequence content) {
                warnings.add(content);
            }

        });
        mojo.execute();
        assertTrue(warnings.isEmpty());
    }

    public void testUseProjectSettingsSetToTrueWithMissingPrefsFile() throws Exception {
        File basedir = getBasedir("projects/projectSettings/p002");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project = projects.get(0);
        AbstractOsgiCompilerMojo mojo = getMojo(projects, project);
        setVariableValueToObject(mojo, "useProjectSettings", Boolean.TRUE);
        final List<CharSequence> warnings = new ArrayList<>();
        mojo.setLog(new SystemStreamLog() {

            @Override
            public void warn(CharSequence content) {
                warnings.add(content);
            }

        });
        mojo.execute();
        assertThat((String) warnings.iterator().next(),
                containsString("Parameter 'useProjectSettings' is set to true, but preferences file"));
    }

    public void test367431_frameworkExtensionCompileAccessRules() throws Exception {
        // This test assumes that the internal class com.sun.crypto.provider.SunJCE exists and its package is not exported.
        // This is the case for all supported JDKs to date (1.8, 11, 14).
        // Note: The bundle uses BREE 1.8 here, because apparently this kind of framework-extension does not
        // correctly work with modular API (Java9+).
        File basedir = getBasedir("projects/367431_frameworkExtensionCompileAccessRules/bundle");
        List<MavenProject> projects = getSortedProjects(basedir,
                new File("src/test/resources/projects/367431_frameworkExtensionCompileAccessRules/repository"));

        MavenProject project = projects.get(0);
        getMojo(projects, project).execute();
    }

    public void testBreeCompilerTargetCompatibilityIsChecked() throws Exception {
        File basedir = getBasedir("projects/bree-target-compatibility");
        List<MavenProject> projects = getSortedProjects(basedir);

        MavenProject project = projects.get(0);
        try {
            getMojo(projects, project).execute();
            fail();
        } catch (MojoExecutionException e) {
            // assert that the compiler mojo checks the target levels of all BREEs (and not just the first or "minimal" one) 
            assertThat(e.getMessage(), containsString(
                    "The effective compiler target level 1.5 is incompatible with the following OSGi execution environments"));
            assertThat(e.getMessage(), containsString("J2SE-1.2"));
            assertThat(e.getMessage(), containsString("CDC-1.0/Foundation-1.0"));
            assertThat(e.getMessage(), containsString("OSGi/Minimum-1.2"));
            assertThat(e.getMessage(), not(containsString("JavaSE-1.6")));
        }
    }

    public void test386210_compilerConfigurationCrosstalk() throws Exception {
        File basedir = getBasedir("projects/crosstalk");
        List<MavenProject> projects = getSortedProjects(basedir);

        getMojo(projects, projects.get(1)).execute();
        getMojo(projects, projects.get(2)).execute();
    }

    public void testCompilerLogWithMultiJarInSingleDirectory() throws Exception {
        File basedir = getBasedir("projects/logs/multiJarSingleDir");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project = projects.get(0);
        lookupConfiguredMojo(project, "compile").execute();
        assertTrue(new File(basedir, "target/log-dir/@dot.log").canRead());
        assertTrue(new File(basedir, "target/log-dir/library.jar.log").canRead());
    }

    public void testCompilerLogWithMultiJarInSubDirectory() throws Exception {
        File basedir = getBasedir("projects/logs/multiJarMultiDir");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project = projects.get(0);
        lookupConfiguredMojo(project, "compile").execute();
        assertTrue(new File(basedir, "target/log-dir/@dot.log").canRead());
        assertTrue(new File(basedir, "target/log-dir/lib_library.jar.log").canRead());
    }

    public void testCompilerLogWithSingleJar() throws Exception {
        File basedir = getBasedir("projects/logs/singleJar");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project = projects.get(0);
        lookupConfiguredMojo(project, "compile").execute();
        assertTrue(new File(basedir, "target/log-dir/@dot.xml").canRead());
    }

    public void testCompilerLogWithCustomComilerArgs() throws Exception {
        File basedir = getBasedir("projects/logs/customCompilerArgs");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project = projects.get(0);
        lookupConfiguredMojo(project, "compile").execute();
        assertTrue(new File(basedir, "target/@dot.xml").canRead());
    }

    public void testCompilerLogWithCustomComilerArgsAndLog() throws Exception {
        File basedir = getBasedir("projects/logs/customCompilerArgsAndLog");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project = projects.get(0);
        try {
            lookupConfiguredMojo(project, "compile").execute();
            fail();
        } catch (MojoFailureException e) {
            assertThat(e.getMessage(), containsString("Compiler logging is configured by the 'log' compiler"
                    + " plugin parameter and the custom compiler argument '-log'. Only either of them is allowed."));
        }
    }

    public void testJreCompilationProfile() throws Exception {
        File basedir = getBasedir("projects/jreCompilationProfile");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project = projects.get(0);
        AbstractOsgiCompilerMojo mojo = getMojo(Collections.singletonList(project), project);
        mojo.execute();
        assertEquals("1.8", mojo.getSourceLevel());
        assertEquals("1.8", mojo.getTargetLevel());
        assertBytecodeMajorLevel(TARGET_1_8, new File(project.getBasedir(), "target/classes/Test.class"));
    }

    public void testUseJDKBREE() throws Exception {
        File basedir = getBasedir("projects/useJDKBREE");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project = projects.get(0);
        AbstractOsgiCompilerMojo mojo = getMojo(Collections.singletonList(project), project);
        try {
            mojo.execute();
            fail("Mojo should fail because of missing toolchains");
        } catch (MojoExecutionException ex) {
            assertThat(ex.getMessage(), Matchers.allOf(StringContains.containsStringIgnoringCase("toolchain"),
                    StringContains.containsString("JavaSE-1.8")));
        }
    }
}
