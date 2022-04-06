/*
 * 	Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.tycho.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.tycho.classpath.SourcepathEntry;
import org.eclipse.tycho.core.osgitools.project.BuildOutputJar;

/**
 * Compiles application sources with eclipse plugin dependencies
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class OsgiCompilerMojo extends AbstractOsgiCompilerMojo {

    private final class BuildOutputJarSourcepathEntry implements SourcepathEntry {
        private final File sourcesRoot;
        private final BuildOutputJar jar;

        private BuildOutputJarSourcepathEntry(File sourcesRoot, BuildOutputJar jar) {
            this.sourcesRoot = sourcesRoot;
            this.jar = jar;
        }

        @Override
        public File getSourcesRoot() {
            return sourcesRoot;
        }

        @Override
        public File getOutputDirectory() {
            return jar.getOutputDirectory();
        }

        @Override
        public List<String> getIncludes() {
            return null;
        }

        @Override
        public List<String> getExcludes() {
            return jar.getFilesToExclude();
        }
    }

    @Override
    protected void doFinish() throws MojoExecutionException {
        BuildOutputJar dotOutputJar = getEclipsePluginProject().getDotOutputJar();
        if (dotOutputJar != null) {
            project.getArtifact().setFile(dotOutputJar.getOutputDirectory());
        }
    }

    @Override
    public List<SourcepathEntry> getSourcepath() throws MojoExecutionException {
        ArrayList<SourcepathEntry> entries = new ArrayList<>();
        for (BuildOutputJar jar : getEclipsePluginProject().getOutputJars()) {
            for (final File sourcesRoot : jar.getSourceFolders()) {
                SourcepathEntry entry = new BuildOutputJarSourcepathEntry(sourcesRoot, jar);
                entries.add(entry);
            }
        }
        return entries;
    }

}
