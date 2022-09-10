/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.extras.pde;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ClasspathEntry;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.classpath.ClasspathContributor;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.OsgiBundleProject;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.helper.PluginRealmHelper;

/**
 * Builds a .target file describing the dependencies for current project. It differs from
 * <code>maven-dependency-plugin:list</code> in the fact that it does return location to bundles,
 * and not to nested jars (in case bundle contain some).
 */
@Mojo(name = "list-dependencies", defaultPhase = LifecyclePhase.GENERATE_TEST_RESOURCES, requiresProject = true, threadSafe = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ListDependenciesMojo extends AbstractMojo {

    @Parameter(property = "project")
    private MavenProject project;

    @Parameter(property = "skip")
    private boolean skip;

    @Component(role = TychoProject.class)
    private Map<String, TychoProject> projectTypes;

    @Component
    private PluginRealmHelper pluginRealmHelper;

    @Parameter(property = "session", readonly = true)
    private MavenSession session;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Execution was skipped");
            return;
        }
        Path outputFile = Path.of(project.getBuild().getDirectory(), "dependencies-list.txt");

        Set<File> dependencyPaths = collectProjectDependencyPaths(project, projectTypes, pluginRealmHelper, session);
        Iterable<String> paths = dependencyPaths.stream().map(File::getAbsolutePath)::iterator;
        try {
            Files.write(outputFile, paths);
        } catch (IOException e) {
            getLog().error(e);
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    //TODO: wrap this into an own component to make it re-usable by others?!
    public static Set<File> collectProjectDependencyPaths(MavenProject project, Map<String, TychoProject> projectTypes,
            PluginRealmHelper pluginRealmHelper, MavenSession session) throws MojoExecutionException {
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        Set<File> dependencies = TychoProjectUtils.getDependencyArtifacts(reactorProject).getArtifacts().stream()
                .map(d -> d.getMavenProject() == null //
                        ? d.getLocation(true)
                        : d.getMavenProject().getArtifact(d.getClassifier()))
                .collect(Collectors.toCollection(HashSet::new));
        if (projectTypes.get(project.getPackaging()) instanceof OsgiBundleProject) {
            try {
                pluginRealmHelper.visitPluginExtensions(project, session, ClasspathContributor.class, cpc -> {
                    List<ClasspathEntry> list = cpc.getAdditionalClasspathEntries(project, Artifact.SCOPE_COMPILE);
                    list.forEach(e -> dependencies.addAll(e.getLocations()));
                });
            } catch (Exception e) {
                throw new MojoExecutionException("can't call classpath contributors", e);
            }
        }
        dependencies.remove(project.getBasedir()); // remove self
        dependencies.remove(null);
        return dependencies;
    }

}
