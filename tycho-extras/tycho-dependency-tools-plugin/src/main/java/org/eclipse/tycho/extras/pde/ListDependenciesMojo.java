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
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.utils.TychoProjectUtils;

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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipped");
            return;
        }
        File outputFile = new File(project.getBuild().getDirectory(), "dependencies-list.txt");
        try {
            outputFile.getParentFile().mkdirs();
            outputFile.createNewFile();
        } catch (IOException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath())) {
            List<ArtifactDescriptor> dependencies = TychoProjectUtils
                    .getDependencyArtifacts(DefaultReactorProject.adapt(project)).getArtifacts().stream()
                    .filter(desc -> !desc.getLocation(true).equals(project.getBasedir())) // remove self
                    .collect(Collectors.toList());
            for (ArtifactDescriptor dependnecy : dependencies) {
                if (dependnecy.getMavenProject() == null) {
                    writer.write(dependnecy.getLocation(true).getAbsolutePath());
                } else {
                    ReactorProject otherProject = dependnecy.getMavenProject();
                    writer.write(otherProject.getArtifact(dependnecy.getClassifier()).getAbsolutePath());
                }
                writer.write('\n');
            }
        } catch (IOException e) {
            getLog().error(e);
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

}
