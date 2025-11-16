/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.tycho.p2maven.ProjectDependencyClosureGraph.Edge;

/**
 * Utility class to dump dependencies graphs as dot files for visualization
 */
public class DotDump {

	/**
	 * Dump the graph to a DOT file for visualization
	 * 
	 * @param file  the file to write the DOT representation to
	 * @param graph the graph to dump
	 * @throws IOException if writing fails
	 */
	public static void dump(File file, ProjectDependencyClosureGraph graph) throws IOException {
		// Detect cycles
		Set<Set<MavenProject>> cycles = graph.detectCycles();

		try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
			writer.println("digraph ProjectDependencies {");
			writer.println("  rankdir=LR;");
			writer.println("  node [shape=box];");
			writer.println();

			// Create a mapping of projects to short names for the graph
			Map<MavenProject, String> projectNames = new HashMap<>();
			int counter = 0;
			for (MavenProject project : graph.projectIUMap.keySet()) {
				String nodeName = "p" + counter++;
				projectNames.put(project, nodeName);
				String label = project.getArtifactId();
				writer.println("  " + nodeName + " [label=\"" + escapeLabel(label) + "\"];");
			}
			writer.println();

			// Write edges with color coding based on cycle type and requirement labels
			for (var entry : graph.projectEdgesMap.entrySet()) {
				MavenProject sourceProject = entry.getKey();
				String sourceName = projectNames.get(sourceProject);

				// Group edges by target project to collect all requirements for each dependency
				Map<MavenProject, EdgeInfo> targetProjectEdges = new HashMap<>();
				for (Edge edge : entry.getValue()) {
					MavenProject targetProject = edge.capability().project();

					// Determine edge color
					String color;
					if (targetProject.equals(sourceProject)) {
						// Self-reference cycle - GRAY
						color = "gray";
					} else if (isInCycle(sourceProject, targetProject, cycles)) {
						// Part of a transitive cycle - RED
						color = "red";
					} else {
						// Normal dependency - BLACK
						color = "black";
					}

					EdgeInfo edgeInfo = targetProjectEdges.computeIfAbsent(targetProject, k -> new EdgeInfo(color));
					edgeInfo.addRequirement(edge.requirement().requirement());
				}

				// Write edge for each target project with requirement labels
				for (var targetEntry : targetProjectEdges.entrySet()) {
					MavenProject targetProject = targetEntry.getKey();
					EdgeInfo edgeInfo = targetEntry.getValue();
					String targetName = projectNames.get(targetProject);
					if (targetName != null) {
						// Build label from requirements
						String label = edgeInfo.getLabel();
						writer.println("  " + sourceName + " -> " + targetName + " [color=" + edgeInfo.color
								+ ", label=\"" + escapeLabel(label) + "\"];");
					}
				}
			}

			writer.println("}");
		}
	}

	/**
	 * Helper class to collect edge information for a dependency relationship
	 */
	private static class EdgeInfo {
		final String color;
		final List<String> requirements = new ArrayList<>();

		EdgeInfo(String color) {
			this.color = color;
		}

		void addRequirement(IRequirement requirement) {
			String reqString = requirement.toString();
			if (!requirements.contains(reqString)) {
				requirements.add(reqString);
			}
		}

		String getLabel() {
			if (requirements.isEmpty()) {
				return "";
			} else if (requirements.size() == 1) {
				return requirements.get(0);
			} else {
				// Join multiple requirements with line breaks
				return String.join("\\n", requirements);
			}
		}
	}

	/**
	 * Check if an edge from source to target is part of a cycle
	 */
	private static boolean isInCycle(MavenProject source, MavenProject target, Set<Set<MavenProject>> cycles) {
		for (Set<MavenProject> cycle : cycles) {
			if (cycle.contains(source) && cycle.contains(target)) {
				return true;
			}
		}
		return false;
	}

	private static String escapeLabel(String label) {
		return label.replace("\\", "\\\\").replace("\"", "\\\"");
	}

}
