/*******************************************************************************
 * Copyright (c) 2022, 2024 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2;

import java.util.AbstractMap.SimpleEntry;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.p2.tools.P2DependencyTreeGenerator;
import org.eclipse.tycho.p2.tools.P2DependencyTreeGenerator.DependencyTreeNode;

/**
 * Similar to dependency:tree outputs a tree of P2 dependencies.
 */
@Mojo(name = "dependency-tree", requiresProject = true, threadSafe = true, requiresDependencyCollection = ResolutionScope.TEST)
public class DependenciesTreeMojo extends AbstractMojo {
    @Parameter(property = "project")
    private MavenProject project;

    @Component
    private P2DependencyTreeGenerator generator;

    @Component
    private TychoProjectManager projectManager;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Optional<TychoProject> tychoProject = projectManager.getTychoProject(project);
        if (tychoProject.isEmpty()) {
            return;
        }

        getLog().info(project.getId());

        List<ArtifactDescriptor> artifacts = tychoProject.get() //
                .getDependencyArtifacts(DefaultReactorProject.adapt(project)) //
                .getArtifacts();
        Map<IInstallableUnit, Set<ReactorProject>> projectMap = artifacts.stream() //
                .filter(a -> a.getMavenProject() != null) //
                .flatMap(a -> a.getInstallableUnits().stream().map(iu -> new SimpleEntry<>(iu, a.getMavenProject()))) //
                .collect(Collectors.groupingBy(Entry::getKey, Collectors.mapping(Entry::getValue, Collectors.toSet())));

        Set<IInstallableUnit> unmapped = new HashSet<>();
        List<DependencyTreeNode> dependencyTree;
        try {
            dependencyTree = generator.buildDependencyTree(project, unmapped);
        } catch (CoreException e) {
            throw new MojoFailureException(e);
        }

        for (DependencyTreeNode rootNode : dependencyTree.stream()
                .sorted(Comparator.comparing(DependencyTreeNode::getInstallableUnit, DependencyTreeNode.COMPARATOR))
                .toList()) {
            printUnit(rootNode, projectMap, 0);
        }

        if (!unmapped.isEmpty()) {
            getLog().info("Units that cannot be matched to any requirement:");
            for (IInstallableUnit unit : unmapped.stream().sorted(DependencyTreeNode.COMPARATOR).toList()) {
                getLog().info("   " + unit.toString());
            }
        }

    }

    private void printUnit(DependencyTreeNode model, Map<IInstallableUnit, Set<ReactorProject>> projectMap,
            int indent) {
        IInstallableUnit unit = model.getInstallableUnit();
        IRequirement satisfies = model.getRequirement();
        StringBuffer line = new StringBuffer();
        for (int i = 0; i < indent; i++) {
            line.append("   ");
        }
        line.append("+- ");
        line.append(unit.getId());
        line.append(" (");
        line.append(unit.getVersion());
        line.append(")");
        if (satisfies != null) {
            line.append(" satisfies ");
            line.append(satisfies);
        }
        Set<ReactorProject> mappedProjects = projectMap.get(unit);
        if (mappedProjects != null && !mappedProjects.isEmpty()) {
            line.append(" --> ");
            line.append(mappedProjects.stream().map(ReactorProject::getId).sorted()
                    .collect(Collectors.joining(", ", "[", "]")));
        }
        getLog().info(line.toString());
        for (DependencyTreeNode child : model.getChildren()) {
            printUnit(child, projectMap, indent + 1);
        }
    }
}
