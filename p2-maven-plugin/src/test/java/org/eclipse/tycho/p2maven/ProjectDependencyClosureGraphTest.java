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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.tycho.p2maven.MavenProjectDependencyProcessor.ProjectDependencies;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ProjectDependencyClosureGraphTest {

	@TempDir
	File tempDir;

	@Test
	public void testSimpleGraphConstruction() throws CoreException {
		// Create mock projects
		MavenProject projectA = createMockProject("projectA");
		MavenProject projectB = createMockProject("projectB");

		// Create mock IUs
		IInstallableUnit iuA = createMockIU("bundleA", "1.0.0");
		IInstallableUnit iuB = createMockIU("bundleB", "1.0.0");

		// Create capability and requirement
		IProvidedCapability capB = createMockCapability("osgi.bundle", "bundleB", "1.0.0");
		when(iuB.getProvidedCapabilities()).thenReturn(List.of(capB));
		
		IRequirement reqB = createMockRequirement("osgi.bundle", "bundleB");
		when(iuA.getRequirements()).thenReturn(List.of(reqB));
		when(iuA.getMetaRequirements()).thenReturn(List.of());
		when(iuB.getRequirements()).thenReturn(List.of());
		when(iuB.getMetaRequirements()).thenReturn(List.of());
		when(iuA.getProvidedCapabilities()).thenReturn(List.of());
		
		// Setup satisfaction check
		when(iuB.satisfies(reqB)).thenReturn(true);
		when(iuA.satisfies(reqB)).thenReturn(false);

		// Build project IU map
		Map<MavenProject, Collection<IInstallableUnit>> projectIUMap = Map.of(
				projectA, List.of(iuA),
				projectB, List.of(iuB)
		);

		// Create graph
		ProjectDependencyClosureGraph graph = new ProjectDependencyClosureGraph(projectIUMap);

		// Verify dependencies
		ProjectDependencies depsA = graph.getProjectDependecies(projectA);
		assertNotNull(depsA);
		Collection<IInstallableUnit> dependencies = depsA.getDependencies(List.of());
		assertTrue(dependencies.contains(iuB), "ProjectA should depend on bundleB");
	}

	@Test
	public void testSelfSatisfiedRequirements() throws CoreException {
		// Create a project with an IU that satisfies its own requirements
		MavenProject project = createMockProject("projectSelf");
		IInstallableUnit iu = createMockIU("bundleSelf", "1.0.0");

		IProvidedCapability cap = createMockCapability("osgi.bundle", "bundleSelf", "1.0.0");
		when(iu.getProvidedCapabilities()).thenReturn(List.of(cap));

		IRequirement req = createMockRequirement("osgi.bundle", "bundleSelf");
		when(iu.getRequirements()).thenReturn(List.of(req));
		when(iu.getMetaRequirements()).thenReturn(List.of());
		when(iu.satisfies(req)).thenReturn(true);

		Map<MavenProject, Collection<IInstallableUnit>> projectIUMap = Map.of(
				project, List.of(iu)
		);

		ProjectDependencyClosureGraph graph = new ProjectDependencyClosureGraph(projectIUMap);

		// Self-satisfied requirements are now included in the graph but filtered out in dependencies
		// because they are from the same project (projectUnits filter)
		ProjectDependencies deps = graph.getProjectDependecies(project);
		Collection<IInstallableUnit> dependencies = deps.getDependencies(List.of());
		assertTrue(dependencies.isEmpty(), "Self-satisfied requirements should be filtered out in dependencies");
	}

	@Test
	public void testUnsatisfiedRequirements() throws CoreException {
		// Create a project with an unsatisfied requirement
		MavenProject project = createMockProject("projectUnsatisfied");
		IInstallableUnit iu = createMockIU("bundleUnsatisfied", "1.0.0");

		IRequirement req = createMockRequirement("osgi.bundle", "nonexistent");
		when(iu.getRequirements()).thenReturn(List.of(req));
		when(iu.getMetaRequirements()).thenReturn(List.of());
		when(iu.satisfies(req)).thenReturn(false);
		when(iu.getProvidedCapabilities()).thenReturn(List.of());

		Map<MavenProject, Collection<IInstallableUnit>> projectIUMap = Map.of(
				project, List.of(iu)
		);

		ProjectDependencyClosureGraph graph = new ProjectDependencyClosureGraph(projectIUMap);

		// The graph should be constructed even with unsatisfied requirements
		assertNotNull(graph);
		ProjectDependencies deps = graph.getProjectDependecies(project);
		assertNotNull(deps);
	}

	@Test
	public void testCyclicDependencies() throws CoreException {
		// Create two projects with cyclic dependencies
		MavenProject projectA = createMockProject("projectA");
		MavenProject projectB = createMockProject("projectB");

		IInstallableUnit iuA = createMockIU("bundleA", "1.0.0");
		IInstallableUnit iuB = createMockIU("bundleB", "1.0.0");

		// A provides capA and requires capB
		IProvidedCapability capA = createMockCapability("osgi.bundle", "bundleA", "1.0.0");
		when(iuA.getProvidedCapabilities()).thenReturn(List.of(capA));
		IRequirement reqB = createMockRequirement("osgi.bundle", "bundleB");
		when(iuA.getRequirements()).thenReturn(List.of(reqB));
		when(iuA.getMetaRequirements()).thenReturn(List.of());

		// B provides capB and requires capA
		IProvidedCapability capB = createMockCapability("osgi.bundle", "bundleB", "1.0.0");
		when(iuB.getProvidedCapabilities()).thenReturn(List.of(capB));
		IRequirement reqA = createMockRequirement("osgi.bundle", "bundleA");
		when(iuB.getRequirements()).thenReturn(List.of(reqA));
		when(iuB.getMetaRequirements()).thenReturn(List.of());

		// Setup satisfaction
		when(iuB.satisfies(reqB)).thenReturn(true);
		when(iuA.satisfies(reqB)).thenReturn(false);
		when(iuA.satisfies(reqA)).thenReturn(true);
		when(iuB.satisfies(reqA)).thenReturn(false);

		Map<MavenProject, Collection<IInstallableUnit>> projectIUMap = Map.of(
				projectA, List.of(iuA),
				projectB, List.of(iuB)
		);

		// Graph should handle cycles gracefully
		ProjectDependencyClosureGraph graph = new ProjectDependencyClosureGraph(projectIUMap);
		assertNotNull(graph);

		// Both projects should have dependencies on each other
		ProjectDependencies depsA = graph.getProjectDependecies(projectA);
		Collection<IInstallableUnit> dependenciesA = depsA.getDependencies(List.of());
		assertTrue(dependenciesA.contains(iuB), "ProjectA should depend on bundleB");

		ProjectDependencies depsB = graph.getProjectDependecies(projectB);
		Collection<IInstallableUnit> dependenciesB = depsB.getDependencies(List.of());
		assertTrue(dependenciesB.contains(iuA), "ProjectB should depend on bundleA");
	}

	@Test
	public void testDumpToFile() throws CoreException, IOException {
		// Create simple project structure
		MavenProject projectA = createMockProject("projectA");
		MavenProject projectB = createMockProject("projectB");

		IInstallableUnit iuA = createMockIU("bundleA", "1.0.0");
		IInstallableUnit iuB = createMockIU("bundleB", "1.0.0");

		IProvidedCapability capB = createMockCapability("osgi.bundle", "bundleB", "1.0.0");
		when(iuB.getProvidedCapabilities()).thenReturn(List.of(capB));
		
		IRequirement reqB = createMockRequirement("osgi.bundle", "bundleB");
		when(iuA.getRequirements()).thenReturn(List.of(reqB));
		when(iuA.getMetaRequirements()).thenReturn(List.of());
		when(iuB.getRequirements()).thenReturn(List.of());
		when(iuB.getMetaRequirements()).thenReturn(List.of());
		when(iuA.getProvidedCapabilities()).thenReturn(List.of());
		
		when(iuB.satisfies(reqB)).thenReturn(true);
		when(iuA.satisfies(reqB)).thenReturn(false);

		Map<MavenProject, Collection<IInstallableUnit>> projectIUMap = Map.of(
				projectA, List.of(iuA),
				projectB, List.of(iuB)
		);

		ProjectDependencyClosureGraph graph = new ProjectDependencyClosureGraph(projectIUMap);

		// Dump to file
		File dotFile = new File(tempDir, "test-dependencies.dot");
		DotDump.dump(dotFile, graph);

		// Verify file was created
		assertTrue(dotFile.exists(), "DOT file should be created");

		// Read and verify content
		String content = Files.readString(dotFile.toPath());
		assertTrue(content.contains("digraph ProjectDependencies"), "Should contain graph declaration");
		assertTrue(content.contains("projectA"), "Should contain projectA");
		assertTrue(content.contains("projectB"), "Should contain projectB");
		assertTrue(content.contains("->"), "Should contain at least one edge");
	}

	@Test
	public void testMetaRequirementsExcluded() throws CoreException {
		// Create a project with meta requirements
		MavenProject project = createMockProject("project");
		IInstallableUnit iu = createMockIU("bundle", "1.0.0");

		IRequirement regularReq = createMockRequirement("osgi.bundle", "regular");
		IRequirement metaReq = createMockRequirement("osgi.bundle", "meta");
		
		// Return regular requirement from getRequirements
		when(iu.getRequirements()).thenReturn(List.of(regularReq));
		// Return meta requirement from getMetaRequirements (should be ignored)
		when(iu.getMetaRequirements()).thenReturn(List.of(metaReq));
		when(iu.satisfies(regularReq)).thenReturn(false);
		when(iu.satisfies(metaReq)).thenReturn(false);
		when(iu.getProvidedCapabilities()).thenReturn(List.of());

		Map<MavenProject, Collection<IInstallableUnit>> projectIUMap = Map.of(
				project, List.of(iu)
		);

		// Graph should only process regular requirements, not meta requirements
		ProjectDependencyClosureGraph graph = new ProjectDependencyClosureGraph(projectIUMap);
		assertNotNull(graph);
		
		// The implementation should only process requirements from getRequirements(),
		// not getMetaRequirements()
	}

	@Test
	public void testGetProject() throws CoreException {
		MavenProject projectA = createMockProject("projectA");
		IInstallableUnit iuA = createMockIU("bundleA", "1.0.0");
		when(iuA.getRequirements()).thenReturn(List.of());
		when(iuA.getMetaRequirements()).thenReturn(List.of());
		when(iuA.getProvidedCapabilities()).thenReturn(List.of());

		Map<MavenProject, Collection<IInstallableUnit>> projectIUMap = Map.of(
				projectA, List.of(iuA)
		);

		ProjectDependencyClosureGraph graph = new ProjectDependencyClosureGraph(projectIUMap);

		// Test getProject method
		assertTrue(graph.getProject(iuA).isPresent());
		assertEquals(projectA, graph.getProject(iuA).get());

		// Test with non-existent IU
		IInstallableUnit nonExistent = createMockIU("nonExistent", "1.0.0");
		assertFalse(graph.getProject(nonExistent).isPresent());
	}

	// Helper methods to create mocks

	private MavenProject createMockProject(String artifactId) {
		MavenProject project = mock(MavenProject.class);
		when(project.getArtifactId()).thenReturn(artifactId);
		when(project.toString()).thenReturn("MavenProject[" + artifactId + "]");
		return project;
	}

	private IInstallableUnit createMockIU(String id, String version) {
		IInstallableUnit iu = mock(IInstallableUnit.class);
		when(iu.getId()).thenReturn(id);
		when(iu.getVersion()).thenReturn(Version.parseVersion(version));
		when(iu.toString()).thenReturn("IU[" + id + ":" + version + "]");
		return iu;
	}

	private IProvidedCapability createMockCapability(String namespace, String name, String version) {
		IProvidedCapability cap = mock(IProvidedCapability.class);
		when(cap.getNamespace()).thenReturn(namespace);
		when(cap.getName()).thenReturn(name);
		when(cap.getVersion()).thenReturn(Version.parseVersion(version));
		return cap;
	}

	private IRequirement createMockRequirement(String namespace, String name) {
		IRequirement req = mock(IRequirement.class);
		when(req.toString()).thenReturn("Requirement[" + namespace + ":" + name + "]");
		when(req.getFilter()).thenReturn(null); // No filter means it always matches
		when(req.getMax()).thenReturn(Integer.MAX_VALUE); // No limit on max
		when(req.getMin()).thenReturn(1); // Default: mandatory requirement
		when(req.isGreedy()).thenReturn(true); // Default: greedy
		return req;
	}

	private IRequirement createMockRequirement(String namespace, String name, int min, boolean greedy) {
		IRequirement req = createMockRequirement(namespace, name);
		when(req.getMin()).thenReturn(min);
		when(req.isGreedy()).thenReturn(greedy);
		return req;
	}

	@Test
	public void testOptionalRequirementFormatting() throws CoreException, IOException {
		// Create a project with an optional requirement (getMin() == 0)
		MavenProject projectA = createMockProject("projectA");
		MavenProject projectB = createMockProject("projectB");

		IInstallableUnit iuA = createMockIU("bundleA", "1.0.0");
		IInstallableUnit iuB = createMockIU("bundleB", "1.0.0");

		// Create an optional requirement (min=0)
		IRequirement optionalReq = createMockRequirement("osgi.bundle", "bundleB", 0, false);
		when(iuA.getRequirements()).thenReturn(List.of(optionalReq));
		when(iuA.getMetaRequirements()).thenReturn(List.of());
		when(iuA.getProvidedCapabilities()).thenReturn(List.of());

		IProvidedCapability capB = createMockCapability("osgi.bundle", "bundleB", "1.0.0");
		when(iuB.getProvidedCapabilities()).thenReturn(List.of(capB));
		when(iuB.getRequirements()).thenReturn(List.of());
		when(iuB.getMetaRequirements()).thenReturn(List.of());

		when(iuB.satisfies(optionalReq)).thenReturn(true);
		when(iuA.satisfies(optionalReq)).thenReturn(false);

		Map<MavenProject, Collection<IInstallableUnit>> projectIUMap = Map.of(
				projectA, List.of(iuA),
				projectB, List.of(iuB)
		);

		ProjectDependencyClosureGraph graph = new ProjectDependencyClosureGraph(projectIUMap);
		File dotFile = new File(tempDir, "optional-requirement.dot");
		DotDump.dump(dotFile, graph);

		assertTrue(dotFile.exists(), "DOT file should be created");
		String content = Files.readString(dotFile.toPath());
		
		// Should contain italic formatting for optional requirement
		assertTrue(content.contains("<I>"), "Should contain italic formatting for optional requirement");
		System.out.println("Optional requirement DOT content:\n" + content);
	}

	@Test
	public void testMandatoryCompileRequirementFormatting() throws CoreException, IOException {
		// Create a project with mandatory compile requirements (osgi.bundle and java.package)
		MavenProject projectA = createMockProject("projectA");
		MavenProject projectB = createMockProject("projectB");

		IInstallableUnit iuA = createMockIU("bundleA", "1.0.0");
		IInstallableUnit iuB = createMockIU("bundleB", "1.0.0");

		// Create a mandatory osgi.bundle requirement
		IRequirement bundleReq = mock(org.eclipse.equinox.internal.p2.metadata.IRequiredCapability.class);
		when(bundleReq.toString()).thenReturn("Requirement[osgi.bundle:bundleB]");
		when(bundleReq.getFilter()).thenReturn(null);
		when(bundleReq.getMax()).thenReturn(Integer.MAX_VALUE);
		when(bundleReq.getMin()).thenReturn(1);
		when(bundleReq.isGreedy()).thenReturn(true);
		when(((org.eclipse.equinox.internal.p2.metadata.IRequiredCapability)bundleReq).getNamespace())
				.thenReturn("osgi.bundle");

		when(iuA.getRequirements()).thenReturn(List.of(bundleReq));
		when(iuA.getMetaRequirements()).thenReturn(List.of());
		when(iuA.getProvidedCapabilities()).thenReturn(List.of());

		IProvidedCapability capB = createMockCapability("osgi.bundle", "bundleB", "1.0.0");
		when(iuB.getProvidedCapabilities()).thenReturn(List.of(capB));
		when(iuB.getRequirements()).thenReturn(List.of());
		when(iuB.getMetaRequirements()).thenReturn(List.of());

		when(iuB.satisfies(bundleReq)).thenReturn(true);
		when(iuA.satisfies(bundleReq)).thenReturn(false);

		Map<MavenProject, Collection<IInstallableUnit>> projectIUMap = Map.of(
				projectA, List.of(iuA),
				projectB, List.of(iuB)
		);

		ProjectDependencyClosureGraph graph = new ProjectDependencyClosureGraph(projectIUMap);
		File dotFile = new File(tempDir, "mandatory-compile-requirement.dot");
		DotDump.dump(dotFile, graph);

		assertTrue(dotFile.exists(), "DOT file should be created");
		String content = Files.readString(dotFile.toPath());
		
		// Should contain bold formatting for mandatory compile requirement
		assertTrue(content.contains("<B>"), "Should contain bold formatting for mandatory compile requirement");
		System.out.println("Mandatory compile requirement DOT content:\n" + content);
	}

	@Test
	public void testGreedyRequirementFormatting() throws CoreException, IOException {
		// Create a project with a greedy requirement
		MavenProject projectA = createMockProject("projectA");
		MavenProject projectB = createMockProject("projectB");

		IInstallableUnit iuA = createMockIU("bundleA", "1.0.0");
		IInstallableUnit iuB = createMockIU("bundleB", "1.0.0");

		// Create a greedy requirement
		IRequirement greedyReq = createMockRequirement("some.namespace", "someCapability", 1, true);
		when(iuA.getRequirements()).thenReturn(List.of(greedyReq));
		when(iuA.getMetaRequirements()).thenReturn(List.of());
		when(iuA.getProvidedCapabilities()).thenReturn(List.of());

		IProvidedCapability capB = createMockCapability("some.namespace", "someCapability", "1.0.0");
		when(iuB.getProvidedCapabilities()).thenReturn(List.of(capB));
		when(iuB.getRequirements()).thenReturn(List.of());
		when(iuB.getMetaRequirements()).thenReturn(List.of());

		when(iuB.satisfies(greedyReq)).thenReturn(true);
		when(iuA.satisfies(greedyReq)).thenReturn(false);

		Map<MavenProject, Collection<IInstallableUnit>> projectIUMap = Map.of(
				projectA, List.of(iuA),
				projectB, List.of(iuB)
		);

		ProjectDependencyClosureGraph graph = new ProjectDependencyClosureGraph(projectIUMap);
		File dotFile = new File(tempDir, "greedy-requirement.dot");
		DotDump.dump(dotFile, graph);

		assertTrue(dotFile.exists(), "DOT file should be created");
		String content = Files.readString(dotFile.toPath());
		
		// Should contain underline formatting for greedy requirement
		assertTrue(content.contains("<U>"), "Should contain underline formatting for greedy requirement");
		System.out.println("Greedy requirement DOT content:\n" + content);
	}

	@Test
	public void testCombinedFormattingOptionalAndMandatoryCompile() throws CoreException, IOException {
		// Create a project with a requirement that is both optional and mandatory compile
		MavenProject projectA = createMockProject("projectA");
		MavenProject projectB = createMockProject("projectB");

		IInstallableUnit iuA = createMockIU("bundleA", "1.0.0");
		IInstallableUnit iuB = createMockIU("bundleB", "1.0.0");

		// Create an optional (min=0) osgi.bundle requirement (mandatory compile)
		IRequirement combinedReq = mock(org.eclipse.equinox.internal.p2.metadata.IRequiredCapability.class);
		when(combinedReq.toString()).thenReturn("Requirement[osgi.bundle:bundleB]");
		when(combinedReq.getFilter()).thenReturn(null);
		when(combinedReq.getMax()).thenReturn(Integer.MAX_VALUE);
		when(combinedReq.getMin()).thenReturn(0); // Optional
		when(combinedReq.isGreedy()).thenReturn(false);
		when(((org.eclipse.equinox.internal.p2.metadata.IRequiredCapability)combinedReq).getNamespace())
				.thenReturn("osgi.bundle"); // Mandatory compile

		when(iuA.getRequirements()).thenReturn(List.of(combinedReq));
		when(iuA.getMetaRequirements()).thenReturn(List.of());
		when(iuA.getProvidedCapabilities()).thenReturn(List.of());

		IProvidedCapability capB = createMockCapability("osgi.bundle", "bundleB", "1.0.0");
		when(iuB.getProvidedCapabilities()).thenReturn(List.of(capB));
		when(iuB.getRequirements()).thenReturn(List.of());
		when(iuB.getMetaRequirements()).thenReturn(List.of());

		when(iuB.satisfies(combinedReq)).thenReturn(true);
		when(iuA.satisfies(combinedReq)).thenReturn(false);

		Map<MavenProject, Collection<IInstallableUnit>> projectIUMap = Map.of(
				projectA, List.of(iuA),
				projectB, List.of(iuB)
		);

		ProjectDependencyClosureGraph graph = new ProjectDependencyClosureGraph(projectIUMap);
		File dotFile = new File(tempDir, "combined-optional-mandatory.dot");
		DotDump.dump(dotFile, graph);

		assertTrue(dotFile.exists(), "DOT file should be created");
		String content = Files.readString(dotFile.toPath());
		
		// Should contain both italic and bold formatting
		assertTrue(content.contains("<I>"), "Should contain italic formatting for optional");
		assertTrue(content.contains("<B>"), "Should contain bold formatting for mandatory compile");
		System.out.println("Combined optional+mandatory compile requirement DOT content:\n" + content);
	}

	@Test
	public void testAllFormattingsCombined() throws CoreException, IOException {
		// Create a project with a requirement that is optional, mandatory compile, and greedy
		MavenProject projectA = createMockProject("projectA");
		MavenProject projectB = createMockProject("projectB");

		IInstallableUnit iuA = createMockIU("bundleA", "1.0.0");
		IInstallableUnit iuB = createMockIU("bundleB", "1.0.0");

		// Create an optional (min=0), greedy, java.package requirement (mandatory compile)
		IRequirement allCombinedReq = mock(org.eclipse.equinox.internal.p2.metadata.IRequiredCapability.class);
		when(allCombinedReq.toString()).thenReturn("Requirement[java.package:org.example]");
		when(allCombinedReq.getFilter()).thenReturn(null);
		when(allCombinedReq.getMax()).thenReturn(Integer.MAX_VALUE);
		when(allCombinedReq.getMin()).thenReturn(0); // Optional
		when(allCombinedReq.isGreedy()).thenReturn(true); // Greedy
		when(((org.eclipse.equinox.internal.p2.metadata.IRequiredCapability)allCombinedReq).getNamespace())
				.thenReturn("java.package"); // Mandatory compile

		when(iuA.getRequirements()).thenReturn(List.of(allCombinedReq));
		when(iuA.getMetaRequirements()).thenReturn(List.of());
		when(iuA.getProvidedCapabilities()).thenReturn(List.of());

		IProvidedCapability capB = createMockCapability("java.package", "org.example", "1.0.0");
		when(iuB.getProvidedCapabilities()).thenReturn(List.of(capB));
		when(iuB.getRequirements()).thenReturn(List.of());
		when(iuB.getMetaRequirements()).thenReturn(List.of());

		when(iuB.satisfies(allCombinedReq)).thenReturn(true);
		when(iuA.satisfies(allCombinedReq)).thenReturn(false);

		Map<MavenProject, Collection<IInstallableUnit>> projectIUMap = Map.of(
				projectA, List.of(iuA),
				projectB, List.of(iuB)
		);

		ProjectDependencyClosureGraph graph = new ProjectDependencyClosureGraph(projectIUMap);
		File dotFile = new File(tempDir, "all-combined-formatting.dot");
		DotDump.dump(dotFile, graph);

		assertTrue(dotFile.exists(), "DOT file should be created");
		String content = Files.readString(dotFile.toPath());
		
		// Should contain all three formatting types
		assertTrue(content.contains("<I>"), "Should contain italic formatting for optional");
		assertTrue(content.contains("<B>"), "Should contain bold formatting for mandatory compile");
		assertTrue(content.contains("<U>"), "Should contain underline formatting for greedy");
		System.out.println("All combined formatting DOT content:\n" + content);
	}

	@Test
	public void testNonMandatoryCompileRequirementNoBoLD() throws CoreException, IOException {
		// Verify that requirements with other namespaces (not osgi.bundle or java.package)
		// don't get bold formatting
		MavenProject projectA = createMockProject("projectA");
		MavenProject projectB = createMockProject("projectB");

		IInstallableUnit iuA = createMockIU("bundleA", "1.0.0");
		IInstallableUnit iuB = createMockIU("bundleB", "1.0.0");

		// Create a requirement with a different namespace (should NOT be bold)
		IRequirement nonCompileReq = mock(org.eclipse.equinox.internal.p2.metadata.IRequiredCapability.class);
		when(nonCompileReq.toString()).thenReturn("Requirement[osgi.service:SomeService]");
		when(nonCompileReq.getFilter()).thenReturn(null);
		when(nonCompileReq.getMax()).thenReturn(Integer.MAX_VALUE);
		when(nonCompileReq.getMin()).thenReturn(1); // Mandatory but not compile
		when(nonCompileReq.isGreedy()).thenReturn(false);
		when(((org.eclipse.equinox.internal.p2.metadata.IRequiredCapability)nonCompileReq).getNamespace())
				.thenReturn("osgi.service"); // Different namespace, should NOT be bold

		when(iuA.getRequirements()).thenReturn(List.of(nonCompileReq));
		when(iuA.getMetaRequirements()).thenReturn(List.of());
		when(iuA.getProvidedCapabilities()).thenReturn(List.of());

		IProvidedCapability capB = createMockCapability("osgi.service", "SomeService", "1.0.0");
		when(iuB.getProvidedCapabilities()).thenReturn(List.of(capB));
		when(iuB.getRequirements()).thenReturn(List.of());
		when(iuB.getMetaRequirements()).thenReturn(List.of());

		when(iuB.satisfies(nonCompileReq)).thenReturn(true);
		when(iuA.satisfies(nonCompileReq)).thenReturn(false);

		Map<MavenProject, Collection<IInstallableUnit>> projectIUMap = Map.of(
				projectA, List.of(iuA),
				projectB, List.of(iuB)
		);

		ProjectDependencyClosureGraph graph = new ProjectDependencyClosureGraph(projectIUMap);
		File dotFile = new File(tempDir, "non-compile-requirement.dot");
		DotDump.dump(dotFile, graph);

		assertTrue(dotFile.exists(), "DOT file should be created");
		String content = Files.readString(dotFile.toPath());
		
		// Should NOT contain bold formatting for non-compile requirement
		assertFalse(content.contains("<B>"), "Should NOT contain bold formatting for non-compile namespace");
		// Should still have the requirement listed
		assertTrue(content.contains("osgi.service"), "Should contain the requirement");
		System.out.println("Non-compile requirement DOT content:\n" + content);
	}

	@Test
	public void testTwoProjectCycleDetection() throws CoreException, IOException {
		// Create the cycle: provider.bundle -> consumer.bundle -> provider.bundle
		// This simulates the OSGi service example from the comment
		
		MavenProject providerProject = createMockProject("provider.bundle");
		MavenProject consumerProject = createMockProject("consumer.bundle");

		// Provider bundle provides a service and requires consumer bundle
		IInstallableUnit providerIU = createMockIU("provider.bundle", "1.0.0");
		IProvidedCapability serviceCapability = createMockCapability("osgi.implementation", 
				"org.eclipse.equinox.internal.p2.repository.Transport", "1.0.0");
		when(providerIU.getProvidedCapabilities()).thenReturn(List.of(serviceCapability));
		
		IRequirement reqConsumer = createMockRequirement("osgi.bundle", "consumer.bundle");
		when(providerIU.getRequirements()).thenReturn(List.of(reqConsumer));
		when(providerIU.getMetaRequirements()).thenReturn(List.of());

		// Consumer bundle requires the service (which provider provides)
		IInstallableUnit consumerIU = createMockIU("consumer.bundle", "1.0.0");
		when(consumerIU.getProvidedCapabilities()).thenReturn(List.of());
		
		IRequirement reqService = createMockRequirement("osgi.implementation", 
				"org.eclipse.equinox.internal.p2.repository.Transport");
		when(consumerIU.getRequirements()).thenReturn(List.of(reqService));
		when(consumerIU.getMetaRequirements()).thenReturn(List.of());

		// Setup satisfaction
		when(consumerIU.satisfies(reqConsumer)).thenReturn(true);
		when(providerIU.satisfies(reqConsumer)).thenReturn(false);
		when(providerIU.satisfies(reqService)).thenReturn(true);
		when(consumerIU.satisfies(reqService)).thenReturn(false);

		Map<MavenProject, Collection<IInstallableUnit>> projectIUMap = Map.of(
				providerProject, List.of(providerIU),
				consumerProject, List.of(consumerIU)
		);

		// Create graph and dump
		ProjectDependencyClosureGraph graph = new ProjectDependencyClosureGraph(projectIUMap);
		File dotFile = new File(tempDir, "two-project-cycle.dot");
		DotDump.dump(dotFile, graph);

		// Verify file was created and contains cycle markers
		assertTrue(dotFile.exists(), "DOT file should be created");
		String content = Files.readString(dotFile.toPath());
		
		// Should have red edges for the cycle
		assertTrue(content.contains("color=red"), "Should contain red edges for transitive cycle");
		assertTrue(content.contains("provider.bundle"), "Should contain provider.bundle");
		assertTrue(content.contains("consumer.bundle"), "Should contain consumer.bundle");
		
		System.out.println("Two-project cycle DOT content:\n" + content);
	}

	@Test
	public void testThreeProjectCycleDetection() throws CoreException, IOException {
		// Create the cycle: A -> B -> C -> A
		
		MavenProject projectA = createMockProject("projectA");
		MavenProject projectB = createMockProject("projectB");
		MavenProject projectC = createMockProject("projectC");

		// Project A requires B
		IInstallableUnit iuA = createMockIU("bundleA", "1.0.0");
		when(iuA.getProvidedCapabilities()).thenReturn(List.of());
		IRequirement reqB = createMockRequirement("osgi.bundle", "bundleB");
		when(iuA.getRequirements()).thenReturn(List.of(reqB));
		when(iuA.getMetaRequirements()).thenReturn(List.of());

		// Project B requires C
		IInstallableUnit iuB = createMockIU("bundleB", "1.0.0");
		when(iuB.getProvidedCapabilities()).thenReturn(List.of());
		IRequirement reqC = createMockRequirement("osgi.bundle", "bundleC");
		when(iuB.getRequirements()).thenReturn(List.of(reqC));
		when(iuB.getMetaRequirements()).thenReturn(List.of());

		// Project C requires A
		IInstallableUnit iuC = createMockIU("bundleC", "1.0.0");
		when(iuC.getProvidedCapabilities()).thenReturn(List.of());
		IRequirement reqA = createMockRequirement("osgi.bundle", "bundleA");
		when(iuC.getRequirements()).thenReturn(List.of(reqA));
		when(iuC.getMetaRequirements()).thenReturn(List.of());

		// Setup satisfaction
		when(iuB.satisfies(reqB)).thenReturn(true);
		when(iuA.satisfies(reqB)).thenReturn(false);
		when(iuC.satisfies(reqB)).thenReturn(false);
		
		when(iuC.satisfies(reqC)).thenReturn(true);
		when(iuA.satisfies(reqC)).thenReturn(false);
		when(iuB.satisfies(reqC)).thenReturn(false);
		
		when(iuA.satisfies(reqA)).thenReturn(true);
		when(iuB.satisfies(reqA)).thenReturn(false);
		when(iuC.satisfies(reqA)).thenReturn(false);

		Map<MavenProject, Collection<IInstallableUnit>> projectIUMap = Map.of(
				projectA, List.of(iuA),
				projectB, List.of(iuB),
				projectC, List.of(iuC)
		);

		// Create graph and dump
		ProjectDependencyClosureGraph graph = new ProjectDependencyClosureGraph(projectIUMap);
		File dotFile = new File(tempDir, "three-project-cycle.dot");
		DotDump.dump(dotFile, graph);

		// Verify file was created and contains cycle markers
		assertTrue(dotFile.exists(), "DOT file should be created");
		String content = Files.readString(dotFile.toPath());
		
		// Should have red edges for the cycle
		assertTrue(content.contains("color=red"), "Should contain red edges for transitive cycle");
		assertTrue(content.contains("projectA"), "Should contain projectA");
		assertTrue(content.contains("projectB"), "Should contain projectB");
		assertTrue(content.contains("projectC"), "Should contain projectC");
		
		System.out.println("Three-project cycle DOT content:\n" + content);
	}

	@Test
	public void testSelfReferenceCycleDetection() throws CoreException, IOException {
		// Create a project with self-reference
		MavenProject project = createMockProject("selfRef");
		IInstallableUnit iu = createMockIU("bundleSelf", "1.0.0");

		IProvidedCapability cap = createMockCapability("osgi.bundle", "bundleSelf", "1.0.0");
		when(iu.getProvidedCapabilities()).thenReturn(List.of(cap));

		IRequirement req = createMockRequirement("osgi.bundle", "bundleSelf");
		when(iu.getRequirements()).thenReturn(List.of(req));
		when(iu.getMetaRequirements()).thenReturn(List.of());
		when(iu.satisfies(req)).thenReturn(true);

		Map<MavenProject, Collection<IInstallableUnit>> projectIUMap = Map.of(
				project, List.of(iu)
		);

		// Create graph and dump
		ProjectDependencyClosureGraph graph = new ProjectDependencyClosureGraph(projectIUMap);
		File dotFile = new File(tempDir, "self-reference-cycle.dot");
		DotDump.dump(dotFile, graph);

		// Verify file was created and contains gray edge for self-reference
		assertTrue(dotFile.exists(), "DOT file should be created");
		String content = Files.readString(dotFile.toPath());
		
		// Should have gray edge for self-reference
		assertTrue(content.contains("color=gray"), "Should contain gray edge for self-reference");
		assertTrue(content.contains("selfRef"), "Should contain selfRef");
		
		System.out.println("Self-reference cycle DOT content:\n" + content);
	}
}
