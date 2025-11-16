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
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.p2maven.ProjectDependencyClosureGraph.Edge;
import org.eclipse.tycho.p2maven.tmp.BundlesAction;

/**
 * Utility class to dump dependencies graphs as dot files for visualization
 */
@SuppressWarnings("restriction")
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
						
						// Check if label contains HTML-like formatting
						if (label.startsWith("<") && label.endsWith(">")) {
							// Use HTML-like label without quotes
							writer.println("  " + sourceName + " -> " + targetName + " [color=" + edgeInfo.color
									+ ", label=" + label + "];");
						} else {
							// Use regular quoted label
							writer.println("  " + sourceName + " -> " + targetName + " [color=" + edgeInfo.color
									+ ", label=\"" + escapeLabel(label) + "\"];");
						}
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
		final List<IRequirement> requirements = new ArrayList<>();

		EdgeInfo(String color) {
			this.color = color;
		}

		void addRequirement(IRequirement requirement) {
			// Check if requirement is already in the list by comparing string representations
			String reqString = requirement.toString();
			boolean alreadyAdded = requirements.stream()
					.anyMatch(r -> r.toString().equals(reqString));
			if (!alreadyAdded) {
				requirements.add(requirement);
			}
		}

		String getLabel() {
			if (requirements.isEmpty()) {
				return "";
			}
			
			// Build formatted label for each requirement
			List<String> formattedRequirements = new ArrayList<>();
			for (IRequirement requirement : requirements) {
				formattedRequirements.add(formatRequirement(requirement));
			}
			
			if (formattedRequirements.size() == 1) {
				return formattedRequirements.get(0);
			} else {
				// Join multiple requirements with line breaks
				return String.join("\\n", formattedRequirements);
			}
		}
		
		/**
		 * Format a requirement with appropriate HTML-like styling based on its properties
		 */
		private static String formatRequirement(IRequirement requirement) {
			String reqText = escapeHtml(requirement.toString());
			
			boolean isOptional = requirement.getMin() == 0;
			boolean isMandatoryCompile = isMandatoryCompileRequirement(requirement);
			boolean isGreedy = requirement.isGreedy();
			
			// Apply formatting if needed
			if (isOptional || isMandatoryCompile || isGreedy) {
				StringBuilder formatted = new StringBuilder("<");
				
				// Apply italic for optional
				if (isOptional) {
					formatted.append("<I>");
				}
				
				// Apply bold for mandatory compile
				if (isMandatoryCompile) {
					formatted.append("<B>");
				}
				
				// Apply underline for greedy
				if (isGreedy) {
					formatted.append("<U>");
				}
				
				formatted.append(reqText);
				
				// Close tags in reverse order
				if (isGreedy) {
					formatted.append("</U>");
				}
				if (isMandatoryCompile) {
					formatted.append("</B>");
				}
				if (isOptional) {
					formatted.append("</I>");
				}
				
				formatted.append(">");
				return formatted.toString();
			}
			
			return reqText;
		}
		
		/**
		 * Check if a requirement is a mandatory compile requirement
		 * (osgi.bundle or java.package namespace)
		 */
		private static boolean isMandatoryCompileRequirement(IRequirement requirement) {
			if (requirement instanceof IRequiredCapability) {
				IRequiredCapability reqCap = (IRequiredCapability) requirement;
				String namespace = reqCap.getNamespace();
				return BundlesAction.CAPABILITY_NS_OSGI_BUNDLE.equals(namespace)
						|| PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE.equals(namespace);
			}
			return false;
		}
		
		/**
		 * Escape HTML special characters for use in HTML-like labels
		 */
		private static String escapeHtml(String text) {
			return text.replace("&", "&amp;")
					.replace("<", "&lt;")
					.replace(">", "&gt;")
					.replace("\"", "&quot;");
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
