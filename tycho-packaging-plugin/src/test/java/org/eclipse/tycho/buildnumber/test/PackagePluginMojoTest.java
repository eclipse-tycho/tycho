/*******************************************************************************
 * Copyright (c) 2010, 2020 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.buildnumber.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.packaging.PackagePluginMojo;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;

public class PackagePluginMojoTest extends AbstractTychoMojoTestCase {

    public void testBinIncludesNoDot() throws Exception {
        File basedir = getBasedir("projects/binIncludesNoDot");
        basedir = new File(basedir, "p001");
        PackagePluginMojo mojo = execMaven(basedir);
        createDummyClassFile(basedir);
        mojo.execute();
        try (JarFile pluginJar = new JarFile(new File(basedir, "target/test.jar"))) {
            assertNull("class files from target/classes must not be included in plugin jar if no '.' in bin.includes",
                    pluginJar.getEntry("TestNoDot.class"));
        }
    }

    public void testOutputClassesInANestedFolder() throws Exception {
        File basedir = getBasedir("projects/outputClassesInANestedFolder");
        //Copy the hello.properties to simulate the compiler and resource mojos
        File classes = new File(basedir, "target/classes/");
        FileUtils.copyFileToDirectory(new File(basedir, "src/main/resources/hello.properties"), classes);
        PackagePluginMojo mojo = execMaven(basedir);
        createDummyClassFile(basedir);
        mojo.execute();
        try (JarFile pluginJar = new JarFile(new File(basedir, "target/test.jar"))) {
            //make sure we can find the WEB-INF/classes/hello.properties
            //and no hello.properties in the root.
            assertNotNull(pluginJar.getEntry("WEB-INF/classes/hello.properties"));
            assertNull(pluginJar.getEntry("hello.properties"));
            //make sure we can find the WEB-INF/classes/TestNoDot.class
            //and no TestNoDot.class in the root.
            assertNotNull(pluginJar.getEntry("WEB-INF/classes/TestNoDot.class"));
            assertNull(pluginJar.getEntry("TestNoDot.class"));
        }
    }

    public void testBinIncludesSpaces() throws Exception {
        File basedir = getBasedir("projects/binIncludesSpaces");
        File classes = new File(basedir, "target/classes");
        classes.mkdirs();
        Files.writeString(new File(classes, "foo.bar").toPath(), "foobar");
        PackagePluginMojo mojo = execMaven(basedir);
        mojo.execute();

        try (JarFile pluginJar = new JarFile(new File(basedir, "target/test.jar"))) {
            assertNotNull(pluginJar.getEntry("foo.bar"));
        }
    }

    public void testCustomManifestNestedJar() throws Exception {
        File basedir = getBasedir("projects/customManifestNestedJar");
        File classes = new File(basedir, "target/classes");
        classes.mkdirs();
        PackagePluginMojo mojo = execMaven(basedir);
        mojo.execute();

        try (JarFile nestedJar = new JarFile(new File(basedir, "nested.jar"))) {
            assertEquals("nested", nestedJar.getManifest().getMainAttributes().getValue("Bundle-SymbolicName"));
        }
    }

    public void testNoManifestVersion() throws Exception {
        File basedir = getBasedir("projects/noManifestVersion");
        PackagePluginMojo mojo = execMaven(basedir);
        mojo.execute();

        Manifest mf;
        try (InputStream is = new FileInputStream(new File(basedir, "target/MANIFEST.MF"))) {
            mf = new Manifest(is);
        }

        String symbolicName = mf.getMainAttributes().getValue("Bundle-SymbolicName");

        assertEquals("bundle;singleton:=true", symbolicName);
    }

    public void testMavenDescriptorNotAddedToJarIfSetToFalse() throws Exception {
        File basedir = getBasedir("projects/addMavenDescriptor/pluginForcedToFalse");
        File classes = new File(basedir, "target/classes");
        classes.mkdirs();
        PackagePluginMojo mojo = execMaven(basedir);
        mojo.execute();

        try (JarFile nestedJar = new JarFile(new File(basedir, "target/pluginForcedToFalse.jar"))) {
            assertNull("Jar must not contain the maven descriptor if forced to not include it!",
                    nestedJar.getEntry("META-INF/maven"));
        }
    }

    public void testMavenDescriptorAddedToJarPerDefault() throws Exception {
        File basedir = getBasedir("projects/addMavenDescriptor/pluginDefault");
        File classes = new File(basedir, "target/classes");
        classes.mkdirs();
        PackagePluginMojo mojo = execMaven(basedir);
        mojo.execute();

        try (JarFile nestedJar = new JarFile(new File(basedir, "target/pluginDefault.jar"))) {
            assertNotNull("Jar must contain the maven descriptor per default!", nestedJar.getEntry("META-INF/maven"));
        }
    }

    private PackagePluginMojo execMaven(File basedir) throws Exception {
		List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project = projects.get(0);
        MavenSession session = newMavenSession(project, projects);

        // set build qualifier
        lookupMojoWithDefaultConfiguration(project, session, "build-qualifier").execute();

        return getMojo("package-plugin", PackagePluginMojo.class, project, session);
    }

    private void createDummyClassFile(File basedir) throws IOException {
        File classFile = new File(basedir, "target/classes/TestNoDot.class");
        classFile.getParentFile().mkdirs();
        classFile.createNewFile();
    }

    private <T> T getMojo(String goal, Class<T> mojoClass, MavenProject project, MavenSession session)
            throws Exception {
        T mojo = mojoClass.cast(lookupMojo(goal, project.getFile()));
        setVariableValueToObject(mojo, "project", project);
        setVariableValueToObject(mojo, "session", session);
        return mojo;
    }

}
