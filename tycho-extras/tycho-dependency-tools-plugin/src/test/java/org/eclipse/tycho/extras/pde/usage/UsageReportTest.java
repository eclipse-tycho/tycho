/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tycho.extras.pde.usage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.maven.project.MavenProject;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.extras.pde.usage.SimpleUsageReportLayout;
import org.eclipse.tycho.extras.pde.usage.UsageReport;
import org.eclipse.tycho.targetplatform.TargetDefinition;
import org.eclipse.tycho.targetplatform.TargetDefinitionContent;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link UsageReport} that verify high-level use cases for dependency
 * usage reporting in target definitions. These tests mock locations and units to
 * assert that the report text contains expected outputs without testing implementation
 * details.
 */
public class UsageReportTest {

    /**
     * Tests that a unit directly used by a project is reported as "used".
     * 
     * <pre>
     * Target Definition
     *   Location L
     *     ├─ Unit A (directly used by project)
     * </pre>
     */
    @Test
    void testDirectlyUsedUnit() {
        UsageReport report = new UsageReport();
        
        // Create mock units
        IInstallableUnit unitA = createMockUnit("unitA", "1.0.0");
        
        // Create target definition with unitA in LocationL
        Map<String, List<IInstallableUnit>> locationUnits = Map.of(
            "LocationL", Arrays.asList(unitA)
        );
        TargetDefinition targetDef = createMockTargetDefinitionWithIULocations("target.target", locationUnits);
        TargetDefinitionContent content = createMockContent(unitA);
        TargetDefinitionResolver resolver = createMockResolver(targetDef, content);
        
        // Analyze the target
        report.analyzeLocations(targetDef, resolver, (l, e) -> {});
        
        // Mark unit A as used
        MavenProject project = createMockProject("project1");
        report.usedUnits.add(unitA);
        report.projectUsage.computeIfAbsent(project, k -> new HashSet<>()).add(unitA);
        
        // Verify unit A is recognized as directly used (not indirectly)
        assertFalse(report.isUsedIndirectly(unitA), "Unit A should be directly used, not indirectly");
        assertTrue(report.isRootUnit(unitA), "Unit A should be a root unit");
    }

    /**
     * Tests that when a transitive dependency is used, the root unit is reported
     * as "INDIRECTLY used" rather than unused.
     * 
     * <pre>
     * Target Definition
     *   Location L
     *     ├─ Unit A
     *        ├─ Requires X
     *        ├─ Requires Y (used by project)
     *        └─ Requires Z
     * </pre>
     */
    @Test
    void testIndirectlyUsedUnit() {
        UsageReport report = new UsageReport();
        
        // Create mock units
        IInstallableUnit unitA = createMockUnit("unitA", "1.0.0");
        IInstallableUnit unitY = createMockUnit("unitY", "1.0.0");
        
        // Set up requirements: A requires Y
        IRequirement reqY = createRequirement("unitY", "1.0.0");
        when(unitA.getRequirements()).thenReturn(Arrays.asList(reqY));
        when(unitY.satisfies(reqY)).thenReturn(true);
        
        // Create target definition with unitA in LocationL
        // analyzeLocations will discover unitY as a dependency of unitA
        Map<String, List<IInstallableUnit>> locationUnits = Map.of(
            "LocationL", Arrays.asList(unitA)
        );
        TargetDefinition targetDef = createMockTargetDefinitionWithIULocations("target.target", locationUnits);
        TargetDefinitionContent content = createMockContent(unitA, unitY);
        TargetDefinitionResolver resolver = createMockResolver(targetDef, content);
        
        // Analyze the target using the proper API
        report.analyzeLocations(targetDef, resolver, (l, e) -> {});
        
        // Mark only Y as used (not A)
        report.usedUnits.add(unitY);
        MavenProject project = createMockProject("project1");
        report.projectUsage.computeIfAbsent(project, k -> new HashSet<>()).add(unitY);
        
        // Verify A is recognized as indirectly used
        assertTrue(report.isUsedIndirectly(unitA), "Unit A should be indirectly used because Y is used");
        assertFalse(report.isUsedIndirectly(unitY), "Unit Y should be directly used");
        
        // Verify indirect usage chain is reported
        String chain = report.getIndirectUsageChain(unitA);
        assertTrue(chain.contains("unitA") && chain.contains("unitY"), 
                "Indirect usage chain should show path from A to Y");
    }

    /**
     * Tests that when no dependencies are used, only the root unit is reported
     * as unused, not the transitive dependencies.
     * 
     * <pre>
     * Target Definition
     *   Location L
     *     ├─ Unit A (unused)
     *        ├─ Requires X (should not be reported separately)
     *        ├─ Requires Y (should not be reported separately)
     *        └─ Requires Z (should not be reported separately)
     * </pre>
     */
    @Test
    void testUnusedUnitWithUnusedDependencies() {
        UsageReport report = new UsageReport();
        
        // Create mock units
        IInstallableUnit unitA = createMockUnit("unitA", "1.0.0");
        IInstallableUnit unitX = createMockUnit("unitX", "1.0.0");
        IInstallableUnit unitY = createMockUnit("unitY", "1.0.0");
        IInstallableUnit unitZ = createMockUnit("unitZ", "1.0.0");
        
        // Set up requirements: A requires X, Y, Z
        IRequirement reqX = createRequirement("unitX", "1.0.0");
        IRequirement reqY = createRequirement("unitY", "1.0.0");
        IRequirement reqZ = createRequirement("unitZ", "1.0.0");
        when(unitA.getRequirements()).thenReturn(Arrays.asList(reqX, reqY, reqZ));
        when(unitX.satisfies(reqX)).thenReturn(true);
        when(unitY.satisfies(reqY)).thenReturn(true);
        when(unitZ.satisfies(reqZ)).thenReturn(true);
        
        // Create target definition with unitA in LocationL
        // analyzeLocations will discover X, Y, Z as dependencies of unitA
        Map<String, List<IInstallableUnit>> locationUnits = Map.of(
            "LocationL", Arrays.asList(unitA)
        );
        TargetDefinition targetDef = createMockTargetDefinitionWithIULocations("target.target", locationUnits);
        TargetDefinitionContent content = createMockContent(unitA, unitX, unitY, unitZ);
        TargetDefinitionResolver resolver = createMockResolver(targetDef, content);
        
        // Analyze the target using the proper API
        report.analyzeLocations(targetDef, resolver, (l, e) -> {});
        
        // Nothing is used
        // Verify A is a root unit but X, Y, Z are not
        assertTrue(report.isRootUnit(unitA), "Unit A should be a root unit");
        assertFalse(report.isRootUnit(unitX), "Unit X should not be a root unit");
        assertFalse(report.isRootUnit(unitY), "Unit Y should not be a root unit");
        assertFalse(report.isRootUnit(unitZ), "Unit Z should not be a root unit");
        
        // Verify A is not indirectly used
        assertFalse(report.isUsedIndirectly(unitA), "Unit A should not be indirectly used");
    }

    /**
     * Tests deeper nesting where a unit requires another unit which has
     * its own requirements.
     * 
     * <pre>
     * Target Definition
     *   Location L
     *     ├─ Unit A
     *        └─ Requires Unit B
     *           ├─ Requires X (used by project)
     *           ├─ Requires Y
     *           └─ Requires Z
     * </pre>
     */
    @Test
    void testDeeperNesting() {
        UsageReport report = new UsageReport();
        
        // Create mock units
        IInstallableUnit unitA = createMockUnit("unitA", "1.0.0");
        IInstallableUnit unitB = createMockUnit("unitB", "1.0.0");
        IInstallableUnit unitX = createMockUnit("unitX", "1.0.0");
        
        // Set up requirements: A requires B, B requires X
        IRequirement reqB = createRequirement("unitB", "1.0.0");
        IRequirement reqX = createRequirement("unitX", "1.0.0");
        when(unitA.getRequirements()).thenReturn(Arrays.asList(reqB));
        when(unitB.getRequirements()).thenReturn(Arrays.asList(reqX));
        when(unitB.satisfies(reqB)).thenReturn(true);
        when(unitX.satisfies(reqX)).thenReturn(true);
        
        // Create target definition with unitA in LocationL
        // analyzeLocations will discover B and X as dependencies
        Map<String, List<IInstallableUnit>> locationUnits = Map.of(
            "LocationL", Arrays.asList(unitA)
        );
        TargetDefinition targetDef = createMockTargetDefinitionWithIULocations("target.target", locationUnits);
        TargetDefinitionContent content = createMockContent(unitA, unitB, unitX);
        TargetDefinitionResolver resolver = createMockResolver(targetDef, content);
        
        // Analyze the target using the proper API
        report.analyzeLocations(targetDef, resolver, (l, e) -> {});
        
        // Mark only X as used
        report.usedUnits.add(unitX);
        MavenProject project = createMockProject("project1");
        report.projectUsage.computeIfAbsent(project, k -> new HashSet<>()).add(unitX);
        
        // Verify both A and B are indirectly used
        assertTrue(report.isUsedIndirectly(unitA), "Unit A should be indirectly used");
        assertTrue(report.isUsedIndirectly(unitB), "Unit B should be indirectly used");
        
        // Verify children collection works transitively
        Set<IInstallableUnit> childrenOfA = report.getAllChildren(unitA);
        assertTrue(childrenOfA.contains(unitB), "Children of A should include B");
        assertTrue(childrenOfA.contains(unitX), "Children of A should include X (transitively)");
    }

    /**
     * Tests that the provenance string includes proper nesting information
     * showing the origin, location, and dependency chain.
     * 
     * <pre>
     * Target Definition (my-target.target)
     *   Location L
     *     ├─ Unit A
     *        └─ Requires Unit B
     * </pre>
     */
    @Test
    void testProvidedByNesting() {
        UsageReport report = new UsageReport();
        
        // Create mock units
        IInstallableUnit unitA = createMockUnit("unitA", "1.0.0");
        IInstallableUnit unitB = createMockUnit("unitB", "1.0.0");
        
        // Set up requirements
        IRequirement reqB = createRequirement("unitB", "1.0.0");
        when(unitA.getRequirements()).thenReturn(Arrays.asList(reqB));
        when(unitB.satisfies(reqB)).thenReturn(true);
        
        // Create target definition with unitA in LocationL
        // analyzeLocations will discover B as a dependency
        Map<String, List<IInstallableUnit>> locationUnits = Map.of(
            "LocationL", Arrays.asList(unitA)
        );
        TargetDefinition targetDef = createMockTargetDefinitionWithIULocations("my-target.target", locationUnits);
        TargetDefinitionContent content = createMockContent(unitA, unitB);
        TargetDefinitionResolver resolver = createMockResolver(targetDef, content);
        
        // Analyze the target using the proper API
        report.analyzeLocations(targetDef, resolver, (l, e) -> {});
        
        // Verify provenance for root unit
        String providedByA = report.getProvidedBy(unitA);
        assertTrue(providedByA.contains("my-target.target"), "Should contain origin");
        assertTrue(providedByA.contains("LocationL"), "Should contain location");
        
        // Verify provenance for nested unit includes path
        String providedByB = report.getProvidedBy(unitB);
        assertTrue(providedByB.contains("my-target.target"), "Should contain origin");
        assertTrue(providedByB.contains("LocationL"), "Should contain location");
    }

    /**
     * Tests that the shortest path from root to a unit is correctly computed.
     * 
     * <pre>
     * Target Definition
     *   Location L
     *     ├─ Unit A
     *        ├─ Requires B
     *        └─ Requires C
     *           └─ Requires B (longer path)
     * </pre>
     */
    @Test
    void testShortestPathFromRoot() {
        UsageReport report = new UsageReport();
        
        // Create mock units
        IInstallableUnit unitA = createMockUnit("unitA", "1.0.0");
        IInstallableUnit unitB = createMockUnit("unitB", "1.0.0");
        IInstallableUnit unitC = createMockUnit("unitC", "1.0.0");
        
        // Set up requirements: A requires B and C, C also requires B
        IRequirement reqB = createRequirement("unitB", "1.0.0");
        IRequirement reqC = createRequirement("unitC", "1.0.0");
        when(unitA.getRequirements()).thenReturn(Arrays.asList(reqB, reqC));
        when(unitC.getRequirements()).thenReturn(Arrays.asList(reqB));
        when(unitB.satisfies(reqB)).thenReturn(true);
        when(unitC.satisfies(reqC)).thenReturn(true);
        
        // Create target definition with unitA in LocationL
        // analyzeLocations will discover B and C as dependencies
        Map<String, List<IInstallableUnit>> locationUnits = Map.of(
            "LocationL", Arrays.asList(unitA)
        );
        TargetDefinition targetDef = createMockTargetDefinitionWithIULocations("target.target", locationUnits);
        TargetDefinitionContent content = createMockContent(unitA, unitB, unitC);
        TargetDefinitionResolver resolver = createMockResolver(targetDef, content);
        
        // Analyze the target using the proper API
        report.analyzeLocations(targetDef, resolver, (l, e) -> {});
        
        // Get shortest path from root to B
        List<IInstallableUnit> path = report.getShortestPathFromRoot(unitB);
        
        // Should be [A, B] (shortest path)
        assertEquals(2, path.size(), "Shortest path should have 2 units");
        assertEquals(unitA, path.get(0), "First unit should be A (root)");
        assertEquals(unitB, path.get(1), "Second unit should be B");
    }

    /**
     * Tests the complete report generation to ensure text output is correct.
     * 
     * <pre>
     * Target Definition
     *   Location L
     *     ├─ Unit A (used directly)
     *     ├─ Unit B (indirectly used via C)
     *        └─ Requires Unit C (used)
     *     └─ Unit D (unused)
     * </pre>
     */
    @Test
    void testReportGeneration() {
        UsageReport report = new UsageReport();
        
        // Create mock units
        IInstallableUnit unitA = createMockUnit("unitA", "1.0.0");
        IInstallableUnit unitB = createMockUnit("unitB", "1.0.0");
        IInstallableUnit unitC = createMockUnit("unitC", "1.0.0");
        IInstallableUnit unitD = createMockUnit("unitD", "1.0.0");
        
        // Set up requirements: B requires C
        IRequirement reqC = createRequirement("unitC", "1.0.0");
        when(unitB.getRequirements()).thenReturn(Arrays.asList(reqC));
        when(unitC.satisfies(reqC)).thenReturn(true);
        
        // Create target definition with all units in LocationL
        Map<String, List<IInstallableUnit>> locationUnits = Map.of(
            "LocationL", Arrays.asList(unitA, unitB, unitC, unitD)
        );
        TargetDefinition targetDef = createMockTargetDefinitionWithIULocations("target.target", locationUnits);
        TargetDefinitionContent content = createMockContent(unitA, unitB, unitC, unitD);
        TargetDefinitionResolver resolver = createMockResolver(targetDef, content);
        
        // Analyze the target
        report.analyzeLocations(targetDef, resolver, (l, e) -> {});
        
        // Mark A and C as used
        MavenProject project = createMockProject("project1");
        report.usedUnits.add(unitA);
        report.usedUnits.add(unitC);
        report.projectUsage.computeIfAbsent(project, k -> new HashSet<>()).add(unitA);
        report.projectUsage.computeIfAbsent(project, k -> new HashSet<>()).add(unitC);
        
        // Collect report output
        List<String> reportLines = new ArrayList<>();
        new SimpleUsageReportLayout().generateReport(report, false, reportLines::add);
        
        // Verify report contains expected elements
        String fullReport = String.join("\n", reportLines);
        assertTrue(fullReport.contains("DEPENDENCIES USAGE REPORT"), "Should contain report header");
        assertTrue(fullReport.contains("unitA") && fullReport.contains("is used by"), 
                "Should report unitA as used");
        assertTrue(fullReport.contains("unitB") && fullReport.contains("INDIRECTLY used"), 
                "Should report unitB as indirectly used");
        assertTrue(fullReport.contains("unitD") && fullReport.contains("UNUSED"), 
                "Should report unitD as unused");
    }

    /**
     * Tests that analyzeLocations correctly handles target definitions with references
     * to other target files.
     * 
     * <pre>
     * TargetA.target
     *   ├─ References TargetB.target
     *   
     * TargetB.target
     *   ├─ Location L
     *      └─ Unit X (used by project)
     * </pre>
     */
    @Test
    void testTargetReferences() throws Exception {
        UsageReport report = new UsageReport();
        
        // Create units
        IInstallableUnit unitX = createMockUnit("unitX", "1.0.0");
        
        // Create target definitions
        TargetDefinition targetA = createMockTargetDefinitionWithReference("targetA.target", "file:///targetB.target");
        TargetDefinition targetB = createMockTargetDefinition("targetB.target");
        
        // Create content for targetB
        TargetDefinitionContent contentB = createMockContent(unitX);
        
        // Create mock resolver
        TargetDefinitionResolver resolver = new TargetDefinitionResolver() {
            @Override
            public TargetDefinition getTargetDefinition(URI uri) {
                if (uri.toString().equals("file:///targetB.target")) {
                    return targetB;
                }
                return null;
            }
            
            @Override
            public TargetDefinitionContent fetchContent(TargetDefinition definition) {
                if (definition == targetB) {
                    return contentB;
                }
                return createMockContent();
            }
        };
        
        // Analyze targetA which references targetB
        report.analyzeLocations(targetA, resolver, (l, e) -> {});
        
        // Verify that targetB is tracked as referenced by targetA
        assertTrue(report.targetReferences.containsKey(targetB), 
                "targetB should be in targetReferences map");
        assertTrue(report.targetReferences.get(targetB).contains(targetA),
                "targetB should be referenced by targetA");
        
        // Verify both targets are in the targetFiles set
        assertTrue(report.getTargetFiles().anyMatch(t -> t.equals(targetA)), "targetA should be in targetFiles");
        assertTrue(report.getTargetFiles().anyMatch(t -> t.equals(targetB)), "targetB should be in targetFiles");
    }
    
    /**
     * Tests that the TreeUsageReportLayout correctly displays referenced target information.
     * 
     * <pre>
     * TargetA.target
     *   ├─ References TargetB.target
     *   ├─ Location L1
     *      └─ Unit A (used)
     *   
     * TargetB.target
     *   ├─ Location L2
     *      └─ Unit B (used)
     * </pre>
     */
    @Test
    @SuppressWarnings("unchecked")
    void testReportWithTargetReferences() throws Exception {
        UsageReport report = new UsageReport();
        
        // Create units
        IInstallableUnit unitA = createMockUnit("unitA", "1.0.0");
        IInstallableUnit unitB = createMockUnit("unitB", "1.0.0");
        
        // Create targetB with IU location
        Map<String, List<IInstallableUnit>> locationUnitsB = Map.of(
            "LocationL2", Arrays.asList(unitB)
        );
        TargetDefinition targetB = createMockTargetDefinitionWithIULocations("targetB.target", locationUnitsB);
        TargetDefinitionContent contentB = createMockContent(unitB);
        
        // Create targetA with both reference to targetB and its own IU location
        TargetDefinition targetA = mock(TargetDefinition.class);
        when(targetA.getOrigin()).thenReturn("targetA.target");
        
        TargetDefinition.TargetReferenceLocation refLocation = mock(TargetDefinition.TargetReferenceLocation.class);
        when(refLocation.getUri()).thenReturn("file:///targetB.target");
        
        // Add IU location for unitA
        TargetDefinition.InstallableUnitLocation iuLocation = mock(TargetDefinition.InstallableUnitLocation.class);
        
        // Store values before stubbing to avoid Mockito UnfinishedStubbingException.
        // Calling getId() and getVersion() on mocks during when() statements creates
        // nested mock invocations that Mockito cannot handle properly.
        String unitAId = unitA.getId();
        String unitAVersion = unitA.getVersion().toString();
        
        TargetDefinition.Unit unitADef = mock(TargetDefinition.Unit.class);
        when(unitADef.getId()).thenReturn(unitAId);
        when(unitADef.getVersion()).thenReturn(unitAVersion);
        when(iuLocation.getUnits()).thenReturn((List) Arrays.asList(unitADef));
        TargetDefinition.Repository repo = mock(TargetDefinition.Repository.class);
        when(repo.getLocation()).thenReturn("LocationL1");
        when(iuLocation.getRepositories()).thenReturn((List) Arrays.asList(repo));
        
        List<TargetDefinition.Location> locationsA = new ArrayList<>();
        locationsA.add(refLocation);
        locationsA.add(iuLocation);
        when(targetA.getLocations()).thenReturn((List) locationsA);
        
        // Create content for targetA
        TargetDefinitionContent contentA = createMockContent(unitA);
        
        // Create resolver that handles both targets
        TargetDefinitionResolver resolver = new TargetDefinitionResolver() {
            @Override
            public TargetDefinition getTargetDefinition(URI uri) {
                if (uri.toString().equals("file:///targetB.target")) {
                    return targetB;
                }
                return null;
            }
            
            @Override
            public TargetDefinitionContent fetchContent(TargetDefinition definition) {
                if (definition == targetA) {
                    return contentA;
                } else if (definition == targetB) {
                    return contentB;
                }
                return createMockContent();
            }
        };
        
        // Analyze targetA which will also analyze targetB
        report.analyzeLocations(targetA, resolver, (l, e) -> {});
        
        // Mark both units as used
        MavenProject project = createMockProject("project1");
        report.usedUnits.add(unitA);
        report.usedUnits.add(unitB);
        report.projectUsage.computeIfAbsent(project, k -> new HashSet<>()).add(unitA);
        report.projectUsage.computeIfAbsent(project, k -> new HashSet<>()).add(unitB);
        
        // Generate report using TreeLayout
        List<String> reportLines = new ArrayList<>();
        new TreeUsageReportLayout().generateReport(report, false, reportLines::add);
        
        String fullReport = String.join("\n", reportLines);
        
        // Verify targetB shows it's referenced by targetA
        assertTrue(fullReport.contains("Referenced in: targetA.target"), 
                "targetB should show it's referenced by targetA");
        
        // Verify targetA shows it references targetB with USED status
        // Note: targetB is USED because unitB is used
        assertTrue(fullReport.contains("References: file:///targetB.target [USED]"), 
                "targetA should show it references targetB with USED status");
    }

    /**
     * Tests reporting of units with shared transitive dependencies.
     * 
     * <pre>
     * Location L1
     *   ├─ Unit A
     *      └─ Requires Unit B
     *         ├─ Requires X (used by project)
     *         ├─ Requires Y (used by project)
     *         └─ Requires Z
     * 
     * Location L2
     *   └─ Unit Q
     *      ├─ Requires P
     *      └─ Requires Y (used by project)
     * </pre>
     * 
     * In this scenario, both A and Q are reported as INDIRECTLY used because they
     * each have children (X and Y for A, Y for Q) that are used by the project.
     * The fact that Y is provided by both units doesn't affect the indirect usage status.
     */
    @Test
    void testSharedTransitiveDependency() {
        UsageReport report = new UsageReport();
        
        // Create mock units for Location L1
        IInstallableUnit unitA = createMockUnit("unitA", "1.0.0");
        IInstallableUnit unitB = createMockUnit("unitB", "1.0.0");
        IInstallableUnit unitX = createMockUnit("unitX", "1.0.0");
        IInstallableUnit unitY = createMockUnit("unitY", "1.0.0");
        IInstallableUnit unitZ = createMockUnit("unitZ", "1.0.0");
        
        // Create mock units for Location L2
        IInstallableUnit unitQ = createMockUnit("unitQ", "1.0.0");
        IInstallableUnit unitP = createMockUnit("unitP", "1.0.0");
        
        // Set up requirements: A requires B, B requires X, Y, Z
        IRequirement reqB = createRequirement("unitB", "1.0.0");
        IRequirement reqX = createRequirement("unitX", "1.0.0");
        IRequirement reqY = createRequirement("unitY", "1.0.0");
        IRequirement reqZ = createRequirement("unitZ", "1.0.0");
        when(unitA.getRequirements()).thenReturn(Arrays.asList(reqB));
        when(unitB.getRequirements()).thenReturn(Arrays.asList(reqX, reqY, reqZ));
        when(unitB.satisfies(reqB)).thenReturn(true);
        when(unitX.satisfies(reqX)).thenReturn(true);
        when(unitY.satisfies(reqY)).thenReturn(true);
        when(unitZ.satisfies(reqZ)).thenReturn(true);
        
        // Set up requirements: Q requires P and Y
        IRequirement reqP = createRequirement("unitP", "1.0.0");
        IRequirement reqYforQ = createRequirement("unitY", "1.0.0");
        when(unitQ.getRequirements()).thenReturn(Arrays.asList(reqP, reqYforQ));
        when(unitP.satisfies(reqP)).thenReturn(true);
        when(unitY.satisfies(reqYforQ)).thenReturn(true);
        
        // Create target definition with units in two locations
        Map<String, List<IInstallableUnit>> locationUnits = Map.of(
            "LocationL1", Arrays.asList(unitA),
            "LocationL2", Arrays.asList(unitQ)
        );
        TargetDefinition targetDef = createMockTargetDefinitionWithIULocations("target.target", locationUnits);
        TargetDefinitionContent content = createMockContent(unitA, unitB, unitX, unitY, unitZ, unitQ, unitP);
        TargetDefinitionResolver resolver = createMockResolver(targetDef, content);
        
        // Analyze the target using the proper API
        report.analyzeLocations(targetDef, resolver, (l, e) -> {});
        
        // Mark X and Y as used
        MavenProject project = createMockProject("project1");
        report.usedUnits.add(unitX);
        report.usedUnits.add(unitY);
        report.projectUsage.computeIfAbsent(project, k -> new HashSet<>()).add(unitX);
        report.projectUsage.computeIfAbsent(project, k -> new HashSet<>()).add(unitY);
        
        // Collect report output
        List<String> reportLines = new ArrayList<>();
        new SimpleUsageReportLayout().generateReport(report, false, reportLines::add);
        
        // Verify report contains expected elements
        String fullReport = String.join("\n", reportLines);
        
        // A should be INDIRECTLY used because X and Y are used
        assertTrue(fullReport.contains("unitA") && fullReport.contains("INDIRECTLY used"), 
                "Unit A should be indirectly used through X and Y");
        
        // Q should also be INDIRECTLY used because Y is used. Any unit with used
        // children is marked as indirectly used, regardless of whether those children
        // are also available through other units.
        assertTrue(fullReport.contains("unitQ") && fullReport.contains("INDIRECTLY used"), 
                "Unit Q should be reported as INDIRECTLY used because Y is used");
        
        // Verify Q is a root unit
        assertTrue(report.isRootUnit(unitQ), "Unit Q should be a root unit");
        
        // Verify Q is indirectly used since Y (its child) is used
        assertTrue(report.isUsedIndirectly(unitQ), 
                "Unit Q should be indirectly used since Y (its child) is used");
    }

    // Helper methods for creating mock objects

    private IInstallableUnit createMockUnit(String id, String version) {
        IInstallableUnit unit = mock(IInstallableUnit.class);
        when(unit.getId()).thenReturn(id);
        when(unit.getVersion()).thenReturn(Version.create(version));
        when(unit.toString()).thenReturn(id + "/" + version);
        when(unit.getRequirements()).thenReturn(Arrays.asList());
        return unit;
    }

    private IRequirement createRequirement(String id, String version) {
        return MetadataFactory.createRequirement(
                IInstallableUnit.NAMESPACE_IU_ID,
                id,
                new VersionRange(Version.create(version), true, Version.create(version), true),
                null,
                false,
                false,
                true
        );
    }

    private TargetDefinition createMockTargetDefinition(String origin) {
        TargetDefinition targetDef = mock(TargetDefinition.class);
        when(targetDef.getOrigin()).thenReturn(origin);
        when(targetDef.getLocations()).thenReturn(Arrays.asList());
        return targetDef;
    }
    
    @SuppressWarnings("unchecked")
    private TargetDefinition createMockTargetDefinitionWithIULocations(String origin, 
            Map<String, List<IInstallableUnit>> locationUnits) {
        TargetDefinition targetDef = mock(TargetDefinition.class);
        when(targetDef.getOrigin()).thenReturn(origin);
        
        List<TargetDefinition.Location> locations = new ArrayList<>();
        for (Map.Entry<String, List<IInstallableUnit>> entry : locationUnits.entrySet()) {
            String locationName = entry.getKey();
            List<IInstallableUnit> units = entry.getValue();
            
            TargetDefinition.InstallableUnitLocation iuLocation = mock(TargetDefinition.InstallableUnitLocation.class);
            
            // Create Unit mocks
            List<TargetDefinition.Unit> unitList = new ArrayList<>();
            for (IInstallableUnit iu : units) {
                // Store values before stubbing to avoid nested stubbing issues
                String unitId = iu.getId();
                String unitVersion = iu.getVersion().toString();
                
                TargetDefinition.Unit unit = mock(TargetDefinition.Unit.class);
                when(unit.getId()).thenReturn(unitId);
                when(unit.getVersion()).thenReturn(unitVersion);
                unitList.add(unit);
            }
            when(iuLocation.getUnits()).thenReturn((List) unitList);
            
            // Create repository mock
            TargetDefinition.Repository repo = mock(TargetDefinition.Repository.class);
            when(repo.getLocation()).thenReturn(locationName);
            List<TargetDefinition.Repository> repos = Arrays.asList(repo);
            when(iuLocation.getRepositories()).thenReturn((List) repos);
            
            locations.add(iuLocation);
        }
        
        when(targetDef.getLocations()).thenReturn((List) locations);
        return targetDef;
    }
    
    private TargetDefinitionResolver createMockResolver(TargetDefinition target, TargetDefinitionContent content) {
        return new TargetDefinitionResolver() {
            @Override
            public TargetDefinition getTargetDefinition(URI uri) {
                return null;
            }
            
            @Override
            public TargetDefinitionContent fetchContent(TargetDefinition definition) {
                if (definition == target) {
                    return content;
                }
                return createMockContent();
            }
        };
    }
    
    @SuppressWarnings("unchecked")
    private TargetDefinition createMockTargetDefinitionWithReference(String origin, String refUri) {
        TargetDefinition targetDef = mock(TargetDefinition.class);
        when(targetDef.getOrigin()).thenReturn(origin);
        
        TargetDefinition.TargetReferenceLocation refLocation = mock(TargetDefinition.TargetReferenceLocation.class);
        when(refLocation.getUri()).thenReturn(refUri);
        
        List<TargetDefinition.Location> locations = new ArrayList<>();
        locations.add(refLocation);
        when(targetDef.getLocations()).thenReturn((List) locations);
        return targetDef;
    }

    @SuppressWarnings("unchecked")
    private TargetDefinitionContent createMockContent(IInstallableUnit... units) {
        TargetDefinitionContent content = mock(TargetDefinitionContent.class);
        Set<IInstallableUnit> unitSet = new HashSet<>(Arrays.asList(units));
        
        IQueryResult<IInstallableUnit> allUnitsResult = mock(IQueryResult.class);
        when(allUnitsResult.toSet()).thenReturn(unitSet);
        when(allUnitsResult.stream()).thenReturn(unitSet.stream());
        
        // Handle queries by matching against the IInstallableUnits
        when(content.query(any(), any())).thenAnswer(invocation -> {
            Object queryArg = invocation.getArgument(0);
            
            // Special case for ALL_UNITS query
            if (queryArg == QueryUtil.ALL_UNITS) {
                return allUnitsResult;
            }
            
            // For IU queries, perform the query on our unit set
            Set<IInstallableUnit> matchingUnits = new HashSet<>();
            if (queryArg instanceof org.eclipse.equinox.p2.query.IQuery) {
                org.eclipse.equinox.p2.query.IQuery<IInstallableUnit> query = 
                    (org.eclipse.equinox.p2.query.IQuery<IInstallableUnit>) queryArg;
                
                // Perform the query on our unit set
                IQueryResult<IInstallableUnit> queryResult = query.perform(unitSet.iterator());
                matchingUnits = queryResult.toSet();
            }
            
            IQueryResult<IInstallableUnit> result = mock(IQueryResult.class);
            when(result.toSet()).thenReturn(matchingUnits);
            when(result.stream()).thenReturn(matchingUnits.stream());
            return result;
        });
        
        return content;
    }

    private MavenProject createMockProject(String id) {
        MavenProject project = mock(MavenProject.class);
        when(project.getId()).thenReturn(id);
        return project;
    }
}
