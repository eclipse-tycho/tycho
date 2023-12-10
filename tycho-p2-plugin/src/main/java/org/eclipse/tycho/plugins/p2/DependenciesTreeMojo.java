/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.LegacySupport;
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
import org.eclipse.tycho.p2maven.InstallableUnitGenerator;

/**
 * Similar to dependency:tree outputs a tree of P2 dependencies.
 */
@Mojo(name = "dependency-tree", requiresProject = true, threadSafe = true, requiresDependencyCollection = ResolutionScope.TEST)
public class DependenciesTreeMojo extends AbstractMojo {

    private static final Comparator<IInstallableUnit> COMPARATOR = Comparator.comparing(IInstallableUnit::getId,
            String.CASE_INSENSITIVE_ORDER);
    @Parameter(property = "project")
    private MavenProject project;

    @Component
    private InstallableUnitGenerator generator;

    @Component
    private LegacySupport legacySupport;

    @Component
    private TychoProjectManager projectManager;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        //TODO maybe we can compute a org.apache.maven.shared.dependency.graph.DependencyNode and reuse org.apache.maven.plugins.dependency.tree.TreeMojo wich has a getSerializingDependencyNodeVisitor

        Optional<TychoProject> tychoProject = projectManager.getTychoProject(project);
        if (tychoProject.isEmpty()) {
            return;
        }

        Set<String> written = new HashSet<String>();
        written.add(project.getId());
        getLog().info(project.getId());

        List<ArtifactDescriptor> artifacts = tychoProject.get()
                .getDependencyArtifacts(DefaultReactorProject.adapt(project)).getArtifacts();
        Map<IInstallableUnit, Set<ReactorProject>> projectMap = artifacts.stream()
                .filter(a -> a.getMavenProject() != null).flatMap(a -> {
                    return a.getInstallableUnits().stream().map(iu -> new SimpleEntry<>(iu, a.getMavenProject()));
                })
                .collect(Collectors.groupingBy(Entry::getKey, Collectors.mapping(Entry::getValue, Collectors.toSet())));
        Set<IInstallableUnit> units = artifacts.stream().flatMap(d -> d.getInstallableUnits().stream())
                .collect(Collectors.toCollection(HashSet::new));
        List<IInstallableUnit> initial;
        try {
            initial = new ArrayList<IInstallableUnit>(
                    generator.getInstallableUnits(project, legacySupport.getSession(), false));
        } catch (CoreException e) {
            throw new MojoFailureException(e);
        }
        units.removeAll(initial);
        int size = initial.size();
        for (int i = 0; i < size; i++) {
            IInstallableUnit unit = initial.get(i);
            printUnit(unit, null, units, projectMap, 0, i == size - 1);
        }
        if (!units.isEmpty()) {
            getLog().info("Units that cannot be matched to any requirement:");
            for (IInstallableUnit unit : units) {
                getLog().info(unit.toString());
            }
        }

    }

    private void printUnit(IInstallableUnit unit, IRequirement satisfies, Set<IInstallableUnit> units,
            Map<IInstallableUnit, Set<ReactorProject>> projectMap, int indent, boolean last) {
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
        List<IInstallableUnit> collected = new ArrayList<IInstallableUnit>();
        Map<IInstallableUnit, IRequirement> requirementsMap = new HashMap<IInstallableUnit, IRequirement>();
        Stream.concat(unit.getRequirements().stream(), unit.getMetaRequirements().stream()).forEach(requirement -> {
            for (Iterator<IInstallableUnit> iterator = units.iterator(); iterator.hasNext();) {
                IInstallableUnit other = iterator.next();
                if (other.satisfies(requirement)) {
                    collected.add(other);
                    requirementsMap.put(other, requirement);
                    iterator.remove();
                }
            }
        });
        Collections.sort(collected, COMPARATOR);
        int size = collected.size();
        for (int i = 0; i < size; i++) {
            IInstallableUnit iu = collected.get(i);
            printUnit(iu, requirementsMap.get(iu), units, projectMap, indent + 1, i == size - 1);
        }
    }

}
