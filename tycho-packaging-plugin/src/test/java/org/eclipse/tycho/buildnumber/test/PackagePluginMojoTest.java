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
package org.eclipse.tycho.buildnumber.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.tycho.packaging.PackagePluginMojo;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;

public class PackagePluginMojoTest extends AbstractTychoMojoTestCase {

    public void testBinIncludesNoDot() throws Exception {
        File basedir = getBasedir("projects/binIncludesNoDot");
        basedir = new File(basedir, "p001");
        PackagePluginMojo mojo = execMaven(basedir);
        createDummyClassFile(basedir);
        mojo.execute();
        JarFile pluginJar = new JarFile(new File(basedir, "target/test.jar"));
        try {
            assertNull("class files from target/classes must not be included in plugin jar if no '.' in bin.includes",
                    pluginJar.getEntry("TestNoDot.class"));
        } finally {
            pluginJar.close();
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
        JarFile pluginJar = new JarFile(new File(basedir, "target/test.jar"));
        try {
            //make sure we can find the WEB-INF/classes/hello.properties
            //and no hello.properties in the root.
            assertNotNull(pluginJar.getEntry("WEB-INF/classes/hello.properties"));
            assertNull(pluginJar.getEntry("hello.properties"));
            //make sure we can find the WEB-INF/classes/TestNoDot.class
            //and no TestNoDot.class in the root.
            assertNotNull(pluginJar.getEntry("WEB-INF/classes/TestNoDot.class"));
            assertNull(pluginJar.getEntry("TestNoDot.class"));
        } finally {
            pluginJar.close();
        }
    }

    public void testBinIncludesSpaces() throws Exception {
        File basedir = getBasedir("projects/binIncludesSpaces");
        File classes = new File(basedir, "target/classes");
        classes.mkdirs();
        FileUtils.fileWrite(new File(classes, "foo.bar").getCanonicalPath(), "foobar");
        PackagePluginMojo mojo = execMaven(basedir);
        mojo.execute();

        JarFile pluginJar = new JarFile(new File(basedir, "target/test.jar"));
        try {
            assertNotNull(pluginJar.getEntry("foo.bar"));
        } finally {
            pluginJar.close();
        }
    }

    public void testCustomManifestNestedJar() throws Exception {
        File basedir = getBasedir("projects/customManifestNestedJar");
        File classes = new File(basedir, "target/classes");
        classes.mkdirs();
        PackagePluginMojo mojo = execMaven(basedir);
        mojo.execute();

        JarFile nestedJar = new JarFile(new File(basedir, "nested.jar"));
        try {
            assertEquals("nested", nestedJar.getManifest().getMainAttributes().getValue("Bundle-SymbolicName"));
        } finally {
            nestedJar.close();
        }
    }

    public void testNoManifestVersion() throws Exception {
        File basedir = getBasedir("projects/noManifestVersion");
        PackagePluginMojo mojo = execMaven(basedir);
        mojo.execute();

        Manifest mf;
        InputStream is = new FileInputStream(new File(basedir, "target/MANIFEST.MF"));
        try {
            mf = new Manifest(is);
        } finally {
            IOUtil.close(is);
        }

        String symbolicName = mf.getMainAttributes().getValue("Bundle-SymbolicName");

        assertEquals("bundle;singleton:=true", symbolicName);
    }

    private PackagePluginMojo execMaven(File basedir) throws Exception {
        List<MavenProject> projects = getSortedProjects(basedir, null);
        MavenProject project = projects.get(0);
        PackagePluginMojo mojo = getMojo(project, newMavenSession(project, projects));
        return mojo;
    }

    private void createDummyClassFile(File basedir) throws IOException {
        File classFile = new File(basedir, "target/classes/TestNoDot.class");
        classFile.getParentFile().mkdirs();
        classFile.createNewFile();
    }

    private PackagePluginMojo getMojo(MavenProject project, MavenSession session) throws Exception {
        PackagePluginMojo mojo = (PackagePluginMojo) lookupMojo("package-plugin", project.getFile());
        setVariableValueToObject(mojo, "project", project);
        setVariableValueToObject(mojo, "session", session);
        return mojo;
    }

}
