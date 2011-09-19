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
package org.eclipse.tycho.core.osgitools.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.tycho.ReactorProject;

public class EclipsePluginProjectImpl implements EclipsePluginProject {

    private final ReactorProject project;
    private final Properties buildProperties;

    private final LinkedHashMap<String, BuildOutputJar> outputJars = new LinkedHashMap<String, BuildOutputJar>();
    private final BuildOutputJar dotOutputJar;

    public EclipsePluginProjectImpl(ReactorProject project) throws IOException {
        this.project = project;
        this.buildProperties = loadProperties(project);

        //
        LinkedHashMap<String, BuildOutputJar> jars = new LinkedHashMap<String, BuildOutputJar>();
        String jarsOrder = buildProperties.getProperty("jars.compile.order");
        if (jarsOrder != null) {
            for (String jarName : jarsOrder.split(",")) {
                jars.put(jarName, null);
            }
        }

        List<String> globalExtraClasspath = new ArrayList<String>();
        if (buildProperties.getProperty("jars.extra.classpath") != null)
            globalExtraClasspath.addAll(Arrays.asList(buildProperties.getProperty("jars.extra.classpath").split(",")));

        String dotJarName = null;

        for (Map.Entry<Object, Object> entry : buildProperties.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (!key.startsWith("source.")) {
                continue;
            }

            String jarName = key.substring(7);
            if (jarName.equals(".")) {
                dotJarName = ".";
            } else if (dotJarName == null && jarName.endsWith("/")) {
                //maven project accomodates a single output directory for the bundle's '.'-jar.
                //take the first jarname that is a folder and use it so the maven's output directory
                //matches it in the final archive.
                dotJarName = jarName;
            }

            File outputDirectory = jarName.equals(dotJarName) ? project.getOutputDirectory() : new File(
                    project.getBuildDirectory(), jarName + "-classes");
            List<File> sourceFolders = toFileList(project.getBasedir(), value.split(","));

            List<String> extraEntries = new ArrayList<String>();
            if (buildProperties.getProperty("extra." + jarName) != null) {
                extraEntries.addAll(Arrays.asList(buildProperties.getProperty("extra." + jarName).split(",")));
                extraEntries.addAll(globalExtraClasspath);
            }
            jars.put(jarName, new BuildOutputJar(jarName, outputDirectory, sourceFolders,
                    extraEntries.size() == 0 ? globalExtraClasspath : extraEntries));
        }

        this.dotOutputJar = dotJarName != null ? jars.get(dotJarName) : null;

        for (BuildOutputJar jar : jars.values()) {
            if (jar != null) {
                this.outputJars.put(jar.getName(), jar);
            }
        }
    }

    private List<File> toFileList(File parent, String[] names) throws IOException {
        ArrayList<File> result = new ArrayList<File>();
        for (String name : names) {
            result.add(new File(parent, name.trim()).getCanonicalFile());
        }
        return result;
    }

    private static Properties loadProperties(ReactorProject project) throws IOException {
        File file = new File(project.getBasedir(), "build.properties");

        Properties buildProperties = new Properties();
        if (file.canRead()) {
            InputStream is = new FileInputStream(file);
            try {
                buildProperties.load(is);
            } finally {
                is.close();
            }
        }

//		throw new IllegalArgumentException("Unable to read build.properties file");

        return buildProperties;
    }

    public Properties getBuildProperties() {
        return buildProperties;
    }

    public ReactorProject getMavenProject() {
        return project;
    }

    public List<BuildOutputJar> getOutputJars() {
        return new ArrayList<BuildOutputJar>(outputJars.values());
    }

    public BuildOutputJar getDotOutputJar() {
        return dotOutputJar;
    }

    public Map<String, BuildOutputJar> getOutputJarMap() {
        return outputJars;
    }

}
