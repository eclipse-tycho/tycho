/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - Bug 572416 - Compile all source folders contained in .classpath
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools.project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.tycho.BuildProperties;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.dotClasspath.ProjectClasspathEntry;

public class EclipsePluginProjectImpl implements EclipsePluginProject {

    private final ReactorProject project;
    private final BuildProperties buildProperties;

    private final LinkedHashMap<String, BuildOutputJar> outputJars = new LinkedHashMap<>();
    private final BuildOutputJar dotOutputJar;
    private Collection<ProjectClasspathEntry> classpathEntries;

    public EclipsePluginProjectImpl(ReactorProject project, BuildProperties buildProperties,
            Collection<ProjectClasspathEntry> classpathEntries) throws IOException {
        this.project = project;
        this.buildProperties = buildProperties;
        this.classpathEntries = classpathEntries;

        LinkedHashMap<String, BuildOutputJar> jars = new LinkedHashMap<>();
        for (String jarName : buildProperties.getJarsCompileOrder()) {
            jars.put(jarName, null);
        }

        List<String> extraClasspath = new ArrayList<>();
        extraClasspath.addAll(buildProperties.getJarsExtraClasspath());

        String dotJarName = null;

        Set<Entry<String, List<String>>> jarToSourceFolderEntries = buildProperties.getJarToSourceFolderMap()
                .entrySet();
        for (Entry<String, List<String>> entry : jarToSourceFolderEntries) {
            String jarName = entry.getKey();
            File outputDirectory;
            if (".".equals(jarName) || jarToSourceFolderEntries.size() == 1) {
                // in case of one classpath entry which is not ".", also use standard
                // maven output dir for better interoperability with plain maven plugins
                dotJarName = jarName;
                outputDirectory = project.getBuildDirectory().getOutputDirectory();
            } else {
                String classesDir = jarName;
                if (jarName.endsWith("/")) {
                    classesDir = jarName.substring(0, jarName.length() - 1);
                }
                outputDirectory = project.getBuildDirectory().getChild(classesDir + "-classes");
            }
            List<File> sourceFolders = toFileList(project.getBasedir(), entry.getValue());

            List<String> excludeFiles = buildProperties.getJarToExcludeFileMap().getOrDefault(jarName,
                    Collections.emptyList());

            List<String> jarExtraEntries = buildProperties.getJarToExtraClasspathMap().get(jarName);
            if (jarExtraEntries != null) {
                extraClasspath.addAll(jarExtraEntries);
            }
            jars.put(jarName,
                    new BuildOutputJar(jarName, outputDirectory, sourceFolders, extraClasspath, excludeFiles));
        }

        this.dotOutputJar = dotJarName != null ? jars.get(dotJarName) : null;

        for (BuildOutputJar jar : jars.values()) {
            if (jar != null) {
                this.outputJars.put(jar.getName(), jar);
            }
        }
    }

    private List<File> toFileList(File parent, List<String> names) throws IOException {
        ArrayList<File> result = new ArrayList<>();
        for (String name : names) {
            // don't call getCanonicalFile here because otherwise we'll be forced to call getCanonical* everywhere
            result.add(new File(new File(parent, name).toURI().normalize()));
        }
        return result;
    }

    @Override
    public ReactorProject getMavenProject() {
        return project;
    }

    @Override
    public List<BuildOutputJar> getOutputJars() {
        return new ArrayList<>(outputJars.values());
    }

    @Override
    public Collection<ProjectClasspathEntry> getClasspathEntries() {
        return Collections.unmodifiableCollection(classpathEntries);
    }

    @Override
    public BuildOutputJar getDotOutputJar() {
        return dotOutputJar;
    }

    @Override
    public Map<String, BuildOutputJar> getOutputJarMap() {
        return outputJars;
    }

    @Override
    public BuildProperties getBuildProperties() {
        return buildProperties;
    }

}
