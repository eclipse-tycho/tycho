/*
 * 	Copyright 2021 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.tycho.compiler;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.classpath.SourcepathEntry;
import org.eclipse.tycho.core.dotClasspath.SourceFolderClasspathEntry;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.project.BuildOutputJar;
import org.eclipse.tycho.core.osgitools.project.EclipsePluginProject;

/**
 * Compiles test sources with eclipse plugin dependencies
 */
@Mojo(name = "testCompile", defaultPhase = LifecyclePhase.TEST_COMPILE, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
public class OsgiTestCompilerMojo extends AbstractOsgiCompilerMojo {

    @Override
    public List<SourcepathEntry> getSourcepath() throws MojoExecutionException {
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        return getSourceFolders().map(file -> {
            return new SourcepathEntry() {
                @Override
                public File getSourcesRoot() {
                    return file;
                }

                @Override
                public File getOutputDirectory() {
                    return reactorProject.getBuildDirectory().getTestOutputDirectory();
                }

                @Override
                public List<String> getIncludes() {
                    return null;
                }

                @Override
                public List<String> getExcludes() {
                    return null;
                }
            };
        }).collect(Collectors.toList());

    }

    @Override
    protected void doCompile() throws MojoExecutionException, MojoFailureException {
        List<File> testSourceFolder = getSourceFolders().collect(Collectors.toList());
        if (testSourceFolder.isEmpty()) {
            return;
        }
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        EclipsePluginProject pluginProject = getEclipsePluginProject();
        BuildOutputJar pluginJar = pluginProject.getDotOutputJar();
        compile(new BuildOutputJar(".", reactorProject.getBuildDirectory().getTestOutputDirectory(), testSourceFolder,
                pluginJar.getExtraClasspathEntries(), Collections.emptyList()));
    }

    private Stream<File> getSourceFolders() throws MojoExecutionException {
        return getEclipsePluginProject().getClasspathEntries().stream()
                .filter(SourceFolderClasspathEntry.class::isInstance).map(SourceFolderClasspathEntry.class::cast)
                .filter(e -> Boolean.parseBoolean(e.getAttributes().get("test")))
                .map(SourceFolderClasspathEntry::getSourcePath);
    }

}
