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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.classpath.ClasspathEntry;
import org.eclipse.tycho.classpath.SourcepathEntry;
import org.eclipse.tycho.core.dotClasspath.SourceFolderClasspathEntry;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;

/**
 * Compiles test sources with eclipse plugin dependencies
 */
@Mojo(name = "testCompile", defaultPhase = LifecyclePhase.TEST_COMPILE, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
public class OsgiTestCompilerMojo extends AbstractOsgiCompilerMojo {

    private final class ClasspathSourcepathEntry implements SourcepathEntry {
        private final File sourceDirectory;
        private File outputDirectory;

        private ClasspathSourcepathEntry(File sourceDirectory, File outputDirectory) {
            this.sourceDirectory = sourceDirectory;
            this.outputDirectory = outputDirectory;
        }

        @Override
        public File getSourcesRoot() {
            return sourceDirectory;
        }

        @Override
        public File getOutputDirectory() {
            return outputDirectory;
        }

        @Override
        public List<String> getIncludes() {
            return null;
        }

        @Override
        public List<String> getExcludes() {
            return null;
        }
    }

    @Override
    public List<SourcepathEntry> getSourcepath() throws MojoExecutionException {
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        File testOutputDirectory = reactorProject.getBuildDirectory().getTestOutputDirectory();
        return getSourceFolders().map(file -> new ClasspathSourcepathEntry(file, testOutputDirectory))
                .collect(Collectors.toList());

    }

    private Stream<File> getSourceFolders() throws MojoExecutionException {
        return getEclipsePluginProject().getClasspathEntries().stream()
                .filter(SourceFolderClasspathEntry.class::isInstance).map(SourceFolderClasspathEntry.class::cast)
                .filter(e -> Boolean.parseBoolean(e.getAttributes().get("test")))
                .map(SourceFolderClasspathEntry::getSourcePath);
    }

    @Override
    public List<ClasspathEntry> getClasspath() throws MojoExecutionException {
        List<ClasspathEntry> res = super.getClasspath();
        res.addAll(getBundleProject().getTestClasspath(DefaultReactorProject.adapt(project)));
        return res;
    }

}
