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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ClasspathEntry;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.classpath.ClasspathContributor;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.OsgiBundleProject;
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

    @Component
    private TychoProjectManager projectManager;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Execution was skipped");
            return;
        }
        File outputFile = new File(project.getBuild().getDirectory(), "dependencies-list.txt");
        try {
            outputFile.getParentFile().mkdirs();
            outputFile.createNewFile();
        } catch (IOException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
        Optional<TychoProject> tychoProject = projectManager.getTychoProject(project);
        if (tychoProject.isEmpty()) {
            return;
        }

        Set<String> written = new HashSet<>();
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath())) {
            ReactorProject reactorProject = DefaultReactorProject.adapt(project);
            List<ArtifactDescriptor> dependencies = tychoProject.get().getDependencyArtifacts(reactorProject)
                    .getArtifacts().stream().filter(desc -> !desc.getLocation(true).equals(project.getBasedir())) // remove self
                    .toList();
            for (ArtifactDescriptor dependnecy : dependencies) {
                if (dependnecy.getMavenProject() == null) {
                    File location = dependnecy.getLocation(true);
                    writeLocation(writer, location, written);
                } else {
                    ReactorProject otherProject = dependnecy.getMavenProject();
                    writeLocation(writer, otherProject.getArtifact(dependnecy.getClassifier()), written);
                }
            }
            TychoProject projectType = projectTypes.get(project.getPackaging());
            if (projectType instanceof OsgiBundleProject) {

                try {
                    pluginRealmHelper.visitPluginExtensions(project, session, ClasspathContributor.class, cpc -> {
                        List<ClasspathEntry> list = cpc.getAdditionalClasspathEntries(project, Artifact.SCOPE_COMPILE);
                        if (list != null && !list.isEmpty()) {
                            for (ClasspathEntry entry : list) {
                                for (File locations : entry.getLocations()) {
                                    try {
                                        writeLocation(writer, locations, written);
                                    } catch (IOException e) {
                                        //ignore...
                                    }
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    throw new MojoExecutionException("can't call classpath contributors", e);
                }
            }
        } catch (IOException e) {
            getLog().error(e);
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void writeLocation(BufferedWriter writer, File location, Set<String> written) throws IOException {
        if (location == null) {
            return;
        }
        String path = location.getAbsolutePath();
        if (written.add(path)) {
            writer.write(path);
            writer.write(System.lineSeparator());
        }
    }

}
