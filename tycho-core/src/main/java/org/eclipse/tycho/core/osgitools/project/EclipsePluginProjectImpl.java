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
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.facade.BuildProperties;
import org.eclipse.tycho.core.facade.BuildPropertiesParser;

public class EclipsePluginProjectImpl implements EclipsePluginProject {

    private final ReactorProject project;
    private final BuildProperties buildProperties;

    private final LinkedHashMap<String, BuildOutputJar> outputJars = new LinkedHashMap<String, BuildOutputJar>();
    private final BuildOutputJar dotOutputJar;

    public EclipsePluginProjectImpl(ReactorProject project, BuildPropertiesParser buildPropertiesParser)
            throws IOException {
        this.project = project;
        this.buildProperties = buildPropertiesParser.parse(project.getBasedir());

        LinkedHashMap<String, BuildOutputJar> jars = new LinkedHashMap<String, BuildOutputJar>();
        for (String jarName : buildProperties.getJarsCompileOrder()) {
            jars.put(jarName, null);
        }

        List<String> extraClasspath = new ArrayList<String>();
        extraClasspath.addAll(buildProperties.getJarsExtraClasspath());

        String dotJarName = null;

        for (Entry<String, List<String>> entry : buildProperties.getJarToSourceFolderMap().entrySet()) {
            String jarName = entry.getKey();
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
            List<File> sourceFolders = toFileList(project.getBasedir(), entry.getValue());

            List<String> jarExtraEntries = buildProperties.getJarToExtraClasspathMap().get(jarName);
            if (jarExtraEntries != null) {
                extraClasspath.addAll(jarExtraEntries);
            }
            jars.put(jarName, new BuildOutputJar(jarName, outputDirectory, sourceFolders, extraClasspath));
        }

        this.dotOutputJar = dotJarName != null ? jars.get(dotJarName) : null;

        for (BuildOutputJar jar : jars.values()) {
            if (jar != null) {
                this.outputJars.put(jar.getName(), jar);
            }
        }
    }

    private List<File> toFileList(File parent, List<String> names) throws IOException {
        ArrayList<File> result = new ArrayList<File>();
        for (String name : names) {
            result.add(new File(parent, name).getCanonicalFile());
        }
        return result;
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

    public BuildProperties getBuildProperties() {
        return buildProperties;
    }

}
