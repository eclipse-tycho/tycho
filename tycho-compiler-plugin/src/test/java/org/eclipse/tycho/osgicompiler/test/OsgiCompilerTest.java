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
package org.eclipse.tycho.osgicompiler.test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.classpath.SourcepathEntry;
import org.eclipse.tycho.compiler.AbstractOsgiCompilerMojo;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;

import copied.org.apache.maven.plugin.CompilationFailureException;

public class OsgiCompilerTest extends AbstractTychoMojoTestCase {

    private static final int TARGET_1_4 = 48;
    private static final int TARGET_1_5 = 49;

    protected File storage;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        storage = new File(getBasedir(), "target/storage");
        FileUtils.deleteDirectory(storage);
    }

    private AbstractOsgiCompilerMojo getMojo(List<MavenProject> projects, MavenProject project) throws Exception {
        AbstractOsgiCompilerMojo mojo = (AbstractOsgiCompilerMojo) lookupMojo("compile", project.getFile());
        setVariableValueToObject(mojo, "project", project);
//		setVariableValueToObject(mojo, "storage", storage);
        setVariableValueToObject(mojo, "outputDirectory",
                new File(project.getBuild().getOutputDirectory()).getAbsoluteFile());
        setVariableValueToObject(mojo, "session", newMavenSession(project, projects));

        // tycho-compiler-jdt does not support forked compilation
//		        setVariableValueToObject(mojo, "fork", fork? Boolean.TRUE: Boolean.FALSE);
        return mojo;
    }

    public void testAccessRestrictionCompilationError() throws Exception {
        File basedir = getBasedir("projects/accessrules");
        List<MavenProject> projects = getSortedProjects(basedir, null);

        try {
            for (MavenProject project : projects) {
                if (!"pom".equals(project.getPackaging())) {
                    getMojo(projects, project).execute();
                }
            }
            fail("Restricted package access");
        } catch (MojoFailureException e) {
            assertTrue(e.getLongMessage().contains("P001Impl is not accessible"));
        }
    }

    public void testAccessRulesClasspath() throws Exception {
        File basedir = getBasedir("projects/accessrules");
        List<MavenProject> projects = getSortedProjects(basedir, null);

        getMojo(projects, projects.get(1)).execute();
        getMojo(projects, projects.get(2)).execute();
        getMojo(projects, projects.get(3)).execute();

        MavenProject project = projects.get(4);
        AbstractOsgiCompilerMojo mojo = getMojo(projects, project);
        List<String> cp = mojo.getClasspathElements();
        assertEquals(4, cp.size());
        assertEquals(getClasspathElement(project.getBasedir(), "target/classes", ""), cp.get(0));
        assertEquals(
                getClasspathElement(new File(getBasedir()), "target/projects/accessrules/p001/target/classes",
                        "[+p001/*:?**/*]"), cp.get(1));
        // note that PDE sorts dependencies coming via imported-package by symbolicName_version
        assertEquals(
                getClasspathElement(new File(getBasedir()), "target/projects/accessrules/p003/target/classes",
                        "[+p003/*:?**/*]"), cp.get(2));
        assertEquals(
                getClasspathElement(new File(getBasedir()), "target/projects/accessrules/p004/target/classes",
                        "[+p004/*:?**/*]"), cp.get(3));
    }

    public void testClasspath() throws Exception {
        File basedir = getBasedir("projects/classpath");
        List<MavenProject> projects = getSortedProjects(basedir, new File(getBasedir(),
                "src/test/resources/projects/classpath/platform"));

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
        assertEquals(getClasspathElement(project.getBasedir(), "target/classes", ""), cp.get(0));
        assertEquals(getClasspathElement(new File(getBasedir()), plainJarPath, "[?**/*]"), cp.get(1));
        assertEquals(getClasspathElement(new File(getBasedir()), nestedJarPath, "[?**/*]"), cp.get(2));

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
        String file = new File(base, path).getCanonicalPath();
        return file + accessRules.replace(":", AbstractOsgiCompilerMojo.RULE_SEPARATOR);
    }

    public void test_multisourceP001_viaMojoConfiguration() throws Exception {
        File basedir = getBasedir("projects/multisource/p001");
        List<MavenProject> projects = getSortedProjects(basedir, null);

        MavenProject project = projects.get(0);
        getMojo(projects, project).execute();

        assertTrue(new File(project.getBasedir(), "target/classes/p001/p1/P1.class").canRead());
        assertTrue(new File(project.getBasedir(), "target/classes/p001/p2/P2.class").canRead());
    }

    public void test_multisourceP002_viaBuildProperties() throws Exception {
        File basedir = getBasedir("projects/multisource/p002");
        List<MavenProject> projects = getSortedProjects(basedir, null);

        MavenProject project = projects.get(0);
        getMojo(projects, project).execute();

        assertTrue(new File(project.getBasedir(), "target/classes/p002/p1/P1.class").canRead());
        assertTrue(new File(project.getBasedir(), "target/classes/p002/p2/P2.class").canRead());
    }

    public void test_multipleOutputJars() throws Exception {
        File basedir = getBasedir("projects/multijar");
        List<MavenProject> projects = getSortedProjects(basedir, null);

        MavenProject project = projects.get(0);
        getMojo(projects, project).execute();

        assertTrue(new File(project.getBasedir(), "target/classes/src/Src.class").canRead());
        assertTrue(new File(project.getBasedir(), "target/library.jar-classes/src2/Src2.class").canRead());

        List<SourcepathEntry> sourcepath = getMojo(projects, project).getSourcepath();
        assertEquals(2, sourcepath.size());
    }

    public void test_multipleOutputJars_getSourcepath() throws Exception {
        File basedir = getBasedir("projects/multijar");
        List<MavenProject> projects = getSortedProjects(basedir, null);

        MavenProject project = projects.get(0);

        List<SourcepathEntry> sourcepath = getMojo(projects, project).getSourcepath();
        assertEquals(2, sourcepath.size());
        assertSameFile(new File(project.getBasedir(), "target/classes"), sourcepath.get(0).getOutputDirectory());
        assertSameFile(new File(project.getBasedir(), "src"), sourcepath.get(0).getSourcesRoot());
        assertSameFile(new File(project.getBasedir(), "target/library.jar-classes"), sourcepath.get(1)
                .getOutputDirectory());
        assertSameFile(new File(project.getBasedir(), "src2"), sourcepath.get(1).getSourcesRoot());
    }

    private void assertSameFile(File expected, File actual) throws IOException {
        assertEquals(expected.getCanonicalFile(), actual.getCanonicalFile());
    }

    public void testCopyResources() throws Exception {
        File basedir = getBasedir("projects/resources/p001");
        List<MavenProject> projects = getSortedProjects(basedir, null);
        MavenProject project = projects.get(0);
        getMojo(projects, project).execute();
        assertTrue(new File(project.getBasedir(), "target/classes/testresources/Test.class").canRead());
        assertTrue(new File(project.getBasedir(), "target/classes/testresources/test.properties").canRead());
    }

    public void testExcludeCopyResources() throws Exception {
        File basedir = getBasedir("projects/resources/p002");
        List<MavenProject> projects = getSortedProjects(basedir, null);
        MavenProject project = projects.get(0);
        getMojo(projects, project).execute();
        assertTrue(new File(project.getBasedir(), "target/classes/testresources/Test.class").canRead());
        assertFalse(new File(project.getBasedir(), "target/classes/testresources/Test.aj").canRead());
    }

    public void testExecutionEnvironment() throws Exception {
        File basedir = getBasedir("projects/executionEnvironment");
        List<MavenProject> projects = getSortedProjects(basedir, null);
        MavenProject project;
        // project with neither POM nor MANIFEST configuration => must fallback to 
        // source/target level == 1.5
        project = projects.get(1);
        getMojo(projects, project).execute();
        assertBytecodeMajorLevel(TARGET_1_5, new File(project.getBasedir(), "target/classes/Generic.class"));

        // project with multiple execution envs.
        // Minimum source and target level must be taken
        project = projects.get(2);
        AbstractOsgiCompilerMojo mojo = getMojo(projects, project);
        assertEquals("OSGi/Minimum-1.0", mojo.getExecutionEnvironment());
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
        assertEquals("J2SE-1.5", mojo.getExecutionEnvironment());
        mojo.execute();
        assertBytecodeMajorLevel(TARGET_1_4, new File(project.getBasedir(), "target/classes/Generic.class"));
        // project with both explicit EE configuration in pom.xml and Bundle-RequiredExecutionEnvironment.
        // explicit configuration in the pom.xml win
        project = projects.get(4);
        mojo = getMojo(projects, project);
        assertEquals("J2SE-1.5", mojo.getExecutionEnvironment());
    }

    private void assertBytecodeMajorLevel(int majorLevel, File classFile) throws ClassFormatException, IOException {
        assertTrue(classFile.canRead());
        JavaClass javaClass = new ClassParser(classFile.getAbsolutePath()).parse();
        assertEquals(majorLevel, javaClass.getMajor());
    }

    public void test_TYCHO0400indirectDependencies() throws Exception {
        File basedir = getBasedir("projects/indirectDependencies");
        List<MavenProject> projects = getSortedProjects(basedir, null);

        assertEquals("C", projects.get(1).getArtifactId());
        getMojo(projects, projects.get(1)).execute();

        assertEquals("B", projects.get(2).getArtifactId());
        getMojo(projects, projects.get(2)).execute();

        assertEquals("A", projects.get(3).getArtifactId());
        AbstractOsgiCompilerMojo mojo = getMojo(projects, projects.get(3));
        List<String> cp = mojo.getClasspathElements();
        assertEquals(getClasspathElement(projects.get(1).getBasedir(), "target/classes", "[?**/*]"), cp.get(2));

        mojo.execute();
        assertTrue(new File(projects.get(3).getBasedir(), "target/classes/a/A.class").canRead());
    }

    public void test_embeddedNonClasspath() throws Exception {
        File basedir = getBasedir("projects/embedednonclasspath");
        List<MavenProject> projects = getSortedProjects(basedir, null);

        MavenProject project = projects.get(0);
        getMojo(projects, project).execute();

        assertTrue(new File(project.getBasedir(), "target/classes/src/Src.class").canRead());
        assertTrue(new File(project.getBasedir(), "target/library.jar-classes/src2/Src2.class").canRead());

        List<SourcepathEntry> sourcepath = getMojo(projects, project).getSourcepath();
        assertEquals(2, sourcepath.size());
    }

    public void test_bootclasspathAccessRules() throws Exception {
        File basedir = getBasedir("projects/bootclasspath-accessrules");
        List<MavenProject> projects = getSortedProjects(basedir, null);

        MavenProject project = projects.get(0);
        getMojo(projects, project).execute();
    }
}
