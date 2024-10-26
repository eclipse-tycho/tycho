/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.tycho.ArtifactDescriptor;

/**
 * Supports computation of the shortest dependency chain from the root to a
 * dependency in a given collection of artifacts.
 */
public class DependencyChain {

	private final ArtifactDescriptor root;
	private final Collection<ArtifactDescriptor> artifacts;

	public DependencyChain(ArtifactDescriptor root, Collection<ArtifactDescriptor> artifacts) {
		this.root = root;
		this.artifacts = artifacts;
	}

	public List<ArtifactDescriptor> pathToRoot(ArtifactDescriptor from) {
		Set<ArtifactDescriptor> visited = new HashSet<>();
		Queue<Edge> queue = new ArrayDeque<>();
		queue.add(new Edge(root, null, null));
		while (!queue.isEmpty()) {
			Edge current = queue.remove();
			if (visited.add(current.value)) {
				for (Edge edge : getDepends(current)) {
					if (edge.value == from) {
						return toList(edge);
					}
					queue.add(edge);
				}
			}
		}
		return Collections.emptyList();
	}

	private List<ArtifactDescriptor> toList(Edge edge) {
		List<ArtifactDescriptor> list = new ArrayList<>();
		while (edge != null) {
			list.add(edge.value);
			edge = edge.parent;
		}
		Collections.reverse(list);
		return list;
	}

	private static final record Edge(ArtifactDescriptor value, IRequirement requirement, Edge parent) {

	}

	private List<Edge> getDepends(Edge edge) {
		List<IRequirement> list = requirementsOf(edge.value);
		List<Edge> candidates = new ArrayList<>(32);
		outer: for (ArtifactDescriptor other : artifacts) {
			for (IRequirement requirement : list) {
				if (satisfies(other, requirement)) {
					candidates.add(new Edge(other, requirement, edge));
					continue outer;
				}
			}
		}
		return candidates;
	}

	private List<IRequirement> requirementsOf(ArtifactDescriptor descriptor) {
		return descriptor.getInstallableUnits().stream()
				.flatMap(iu -> Stream.concat(iu.getRequirements().stream(), iu.getMetaRequirements().stream()))
				.distinct().toList();
	}

	private boolean satisfies(ArtifactDescriptor descriptor, IRequirement requirement) {
		return descriptor.getInstallableUnits().stream().anyMatch(iu -> iu.satisfies(requirement));
	}

}
