/*******************************************************************************
 * Copyright (c) 2024 Patrick Ziegler and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Patrick Ziegler - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.p2.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.IDependencyMetadata.DependencyMetadataType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;

/**
 * Utility class for converting a flat dependency into a dependency tree. The tree is structured in
 * such a way that an IU {@code a} is a child of another IU {@code b}, if and only if {@code a} is
 * required by {@code b}. If {@code b} is required by multiple IUs, the first one is selected.<br>
 * Used by e.g. the dependency-tree Mojo, in order to mimic the behavior of the native Maven
 * dependency-tree Mojo.<br>
 * This class is intended to be use as a Plexus component, so that all required fields are
 * automatically initialized using DI.
 */
@Named
@Singleton
public final class P2DependencyTreeGenerator {
    private final TychoProjectManager projectManager;
    private final LegacySupport legacySupport;

    @Inject
    public P2DependencyTreeGenerator(TychoProjectManager projectManager, LegacySupport legacySupport) {
        this.projectManager = projectManager;
        this.legacySupport = legacySupport;
    }

    /**
     * Calculates and returns the dependency tree of the given Maven project. The list that is
     * returned by this method corresponds to the IUs which are directly required by the given
     * project.
     *
     * @param project
     *            One of the Maven projects of the current reactor build. If this project is not a
     *            Tycho project (e.g. the parent pom), an empty list is returned.
     * @param unmapped
     *            A set containing all IUs which could not be added to the dependency tree. Meaning
     *            that those units are required by the project but not by any of its IUs. Must be
     *            mutable.
     * @return as described.
     * @throws CoreException
     *             if anything goes wrong
     */
    public List<DependencyTreeNode> buildDependencyTree(MavenProject project, Set<IInstallableUnit> unmapped)
            throws CoreException {
        //TODO maybe we can compute a org.apache.maven.shared.dependency.graph.DependencyNode and reuse org.apache.maven.plugins.dependency.tree.TreeMojo wich has a getSerializingDependencyNodeVisitor
        Optional<TychoProject> tychoProject = projectManager.getTychoProject(project);
        if (tychoProject.isEmpty()) {
            return Collections.emptyList();
        }

        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        List<ArtifactDescriptor> artifacts = tychoProject.get() //
                .getDependencyArtifacts(reactorProject) //
                .getArtifacts();
        Set<IInstallableUnit> units = artifacts.stream() //
                .flatMap(d -> d.getInstallableUnits().stream()) //
                .collect(Collectors.toCollection(HashSet::new));
        Set<IInstallableUnit> initial = reactorProject.getDependencyMetadata(DependencyMetadataType.INITIAL);
        units.removeAll(initial);

        return Collections.unmodifiableList(DependencyTreeNode.create(initial, units, unmapped));
    }

    /**
     * This class represents a single IU within the dependency tree and holds. Two nodes in this
     * tree are connected (as in parent and child), if and only if the child IU is required by the
     * parent. Each IU is unique and must only appear once in the dependency tree.
     */
    public static class DependencyTreeNode {
        public static final Comparator<IInstallableUnit> COMPARATOR = Comparator.comparing(IInstallableUnit::getId,
                String.CASE_INSENSITIVE_ORDER);
        private final IInstallableUnit iu;
        private final IRequirement satisfies;
        private final List<DependencyTreeNode> children = new ArrayList<>();

        private DependencyTreeNode(IInstallableUnit iu, IRequirement satisfies) {
            this.iu = iu;
            this.satisfies = satisfies;
        }

        public IInstallableUnit getInstallableUnit() {
            return iu;
        }

        public IRequirement getRequirement() {
            return satisfies;
        }

        public List<DependencyTreeNode> getChildren() {
            return Collections.unmodifiableList(children);
        }

        /**
         * Returns the IU (if present) that is contained by this node. Primarily used to make
         * debugging easier.
         */
        @Override
        public String toString() {
            return Objects.toString(iu);
        }

        /**
         * Create the dependency tree based on the {@code initial} IUs. A tree node is created for
         * each IU of {@code initial}. The children of a node correspond to all IUs that are
         * (directly) required by the parent IU. Each IU in {@code units} only appears once, even if
         * it required by multiple IUs.
         *
         * @param initial
         *            The "direct" IUs referenced by a given artifact.
         * @param units
         *            All IUs that are required by a given artifact, excluding {@code initial}.
         * @param unmapped
         *            A subset of {@code units}, which are not contained in the dependency tree.
         *            Meaning that those IUs are not necessarily required to satisfy the
         *            dependencies of an artifact.
         * @return A list of dependency tree models. Each model in this list matches an IU of
         *         {@code initial}.
         */
        private static List<DependencyTreeNode> create(Collection<IInstallableUnit> initial,
                Set<IInstallableUnit> units, Set<IInstallableUnit> unmapped) {
            List<DependencyTreeNode> rootNodes = new ArrayList<>();
            initial.stream().sorted(COMPARATOR).forEach(iu -> {
                DependencyTreeNode rootNode = new DependencyTreeNode(iu, null);
                create(rootNode, units);
                rootNodes.add(rootNode);
            });
            unmapped.addAll(units);
            return rootNodes;
        }

        /**
         * Internal helper method which recursively goes through IUs that are required by the IU
         * held by {@code node}. For each IU that satisfies this requirement a new
         * {@link DependencyTreeNode} is created and added as a child to {@link node}. If such an IU
         * is found, it is removed from {@code units}, meaning that each IU can only show up once in
         * the dependency tree. The children of each node are sorted lexicographically according to
         * {@link #COMPARATOR}.
         *
         * @param node
         *            The (intermediate) head of the dependency tree.
         * @param units
         *            A set of all IUs that are associated with the currently handled project.
         */
        private static void create(DependencyTreeNode node, Set<IInstallableUnit> units) {
            List<IInstallableUnit> collected = new ArrayList<>();
            Map<IInstallableUnit, IRequirement> requirementsMap = new HashMap<>();
            IInstallableUnit unit = node.getInstallableUnit();
            //
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
            //
            Collections.sort(collected, COMPARATOR);
            for (IInstallableUnit iu : collected) {
                IRequirement satisfies = requirementsMap.get(iu);
                DependencyTreeNode childNode = new DependencyTreeNode(iu, satisfies);
                node.children.add(childNode);
                create(childNode, units);
            }
        }
    }
}
