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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.targetplatform.TargetDefinition;

/**
 * Tree-structured report layout that organizes units by target file and location.
 */
@Component(role = ReportLayout.class, hint = "tree")
final class TreeUsageReportLayout implements ReportLayout {

    private final int lineWrapLimit;
    
    /**
     * Synthetic JRE unit ID that should be filtered from reports
     */
    private static final String SYNTHETIC_JRE_UNIT_ID = "a.jre.javase";

    /**
     * Creates a tree layout with the specified line wrap limit.
     * 
     * @param lineWrapLimit
     *            maximum line length before wrapping (default 200)
     */
    public TreeUsageReportLayout(int lineWrapLimit) {
        this.lineWrapLimit = lineWrapLimit;
    }

    /**
     * Creates a tree layout with default line wrap limit of 200 characters.
     */
    public TreeUsageReportLayout() {
        this(200);
    }

    @Override
    public void generateReport(UsageReport report, boolean verbose, Consumer<String> reportConsumer) {
        reportConsumer.accept("###### DEPENDENCIES USAGE REPORT #######");
        reportConsumer.accept("Your build uses " + report.usedUnits.size() + " dependencies.");
        reportConsumer.accept("Your build uses " + report.getTargetFilesCount() + " target file(s).");
        reportConsumer.accept("");

        // Group units by target file and location
        Map<TargetDefinition, Map<String, List<UnitInfo>>> targetStructure = buildTargetStructure(report);

        // Only report on root units (defined directly in target files)
        Set<IInstallableUnit> allUnits = report.providedBy.keySet();
        Set<IInstallableUnit> rootUnits = allUnits.stream().filter(report::isRootUnit).collect(Collectors.toSet());

        // Track which units have been covered by reporting their parent
        Set<IInstallableUnit> reportedUnits = new HashSet<>();
        
        // Track shortest paths for each used unit (for deduplication)
        Map<IInstallableUnit, List<IInstallableUnit>> shortestPaths = computeShortestPaths(report, rootUnits);

        // Generate tree output for each target file
        for (TargetDefinition targetFile : targetStructure.keySet().stream()
                .sorted(Comparator.comparing(TargetDefinition::getOrigin)).toList()) {
            reportConsumer.accept("Target: " + targetFile.getOrigin());
            int totalUnits = report.getTargetDefinitionContent(targetFile).query(QueryUtil.ALL_UNITS, null).toSet().size();
            reportConsumer.accept(
                    "  Total units: " + totalUnits + " from " + targetFile.getLocations().size() + " locations");
            
            // Show if this target is referenced by other targets
            if (report.targetReferences.containsKey(targetFile)) {
                List<TargetDefinition> referencedBy = report.targetReferences.get(targetFile);
                String referencingTargets = referencedBy.stream()
                        .map(TargetDefinition::getOrigin)
                        .collect(Collectors.joining(", "));
                reportConsumer.accept("  Referenced in: " + referencingTargets);
            }
            
            // Show target references (targets that this target references)
            for (TargetDefinition.Location location : targetFile.getLocations()) {
                if (location instanceof TargetDefinition.TargetReferenceLocation refLoc) {
                    String refUri = refLoc.getUri();
                    // Determine if the referenced target is used
                    boolean isUsed = report.isReferencedTargetUsed(refUri);
                    String status = isUsed ? "USED" : "UNUSED";
                    reportConsumer.accept("  References: " + refUri + " [" + status + "]");
                }
            }

            Map<String, List<UnitInfo>> locations = targetStructure.get(targetFile);

            // Sort locations by unique project count (highest to lowest)
            List<String> sortedLocations = sortLocationsByProjectCount(locations, report);

            for (String location : sortedLocations) {
                List<UnitInfo> units = locations.get(location);

                // Filter to only include root units from this location
                List<UnitInfo> rootUnitsInLocation = units.stream()
                        .filter(ui -> rootUnits.contains(ui.unit))
                        .filter(ui -> !reportedUnits.contains(ui.unit))
                        // Filter out synthetic JRE units
                        .filter(ui -> !ui.unit.getId().equals(SYNTHETIC_JRE_UNIT_ID))
                        .toList();

                if (rootUnitsInLocation.isEmpty()) {
                    continue;
                }

                // Count unique projects for this location
                Set<String> locationProjects = countUniqueProjects(rootUnitsInLocation, report);
                
                reportConsumer.accept("  Location: " + wrapLine(location, "    ", lineWrapLimit) + 
                        " (" + locationProjects.size() + " project" + (locationProjects.size() == 1 ? "" : "s") + ")");

                // Sort root units by usage count (most used to least used)
                List<UnitInfo> sortedUnits = sortUnitsByUsage(rootUnitsInLocation, report);

                for (UnitInfo unitInfo : sortedUnits) {
                    IInstallableUnit unit = unitInfo.unit;

                    // Skip if already reported
                    if (reportedUnits.contains(unit)) {
                        continue;
                    }

                    // Determine usage status
                    if (report.usedUnits.contains(unit)) {
                        // USED status - include project count in brackets
                        List<String> projects = report.projectUsage.entrySet().stream()
                                .filter(entry -> entry.getValue().contains(unit))
                                .map(project -> project.getKey().getId()).toList();
                        String status = "USED (" + projects.size() + " project" + (projects.size() == 1 ? "" : "s") + ")";
                        String unitLine = "    • " + formatUnit(unit) + " [" + status + "]";
                        reportConsumer.accept(unitLine);
                        
                        // If verbose, display the project IDs
                        if (verbose) {
                            displayProjectList(projects, reportConsumer, "      ");
                        }
                    } else if (hasUsedChildrenExcludingJRE(unit, report)) {
                        // INDIRECTLY USED status - show as tree
                        // Note: We use hasUsedChildrenExcludingJRE instead of report.isUsedIndirectly
                        // because we need to filter out synthetic JRE dependencies. Units that only
                        // depend on JRE units should show as UNUSED, not INDIRECTLY USED.
                        
                        // Special handling for features
                        boolean isFeature = unit.getId().endsWith(".feature.group");
                        boolean pathContainsFeature = pathToUsedChildContainsFeature(unit, report, shortestPaths);
                        
                        String status = (isFeature && !pathContainsFeature) ? "USED" : "INDIRECTLY USED";
                        String unitLine = "    • " + formatUnit(unit) + " [" + status + "]";
                        reportConsumer.accept(unitLine);
                        
                        // Display indirect usage chain as a tree structure
                        displayIndirectUsageTree(unit, report, reportConsumer, reportedUnits, shortestPaths, verbose);
                    } else {
                        // UNUSED status
                        String unitLine = "    • " + formatUnit(unit) + " [UNUSED]";
                        reportConsumer.accept(unitLine);
                        reportConsumer.accept("      Can potentially be removed");
                    }

                    // Mark this unit and all its transitive dependencies as reported
                    reportedUnits.add(unit);
                    reportedUnits.addAll(report.getAllChildren(unit));
                }
            }

            reportConsumer.accept("");
        }
    }

    /**
     * Wraps a line to fit within the specified limit, continuing on the next line with the
     * specified indent.
     */
    private String wrapLine(String text, String indent, int limit) {
        if (text.length() + indent.length() <= limit) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        String[] parts = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String part : parts) {
            if (currentLine.length() > 0 && currentLine.length() + part.length() + 1 + indent.length() > limit) {
                // Start a new line
                if (result.length() > 0) {
                    result.append("\n").append(indent);
                }
                result.append(currentLine);
                currentLine = new StringBuilder();
            }

            if (currentLine.length() > 0) {
                currentLine.append(" ");
            }
            currentLine.append(part);
        }

        // Append the last line
        if (currentLine.length() > 0) {
            if (result.length() > 0) {
                result.append("\n").append(indent);
            }
            result.append(currentLine);
        }

        return result.toString();
    }

    /**
     * Formats a unit for display, handling special cases like feature groups.
     */
    private String formatUnit(IInstallableUnit unit) {
        String id = unit.getId();
        String version = unit.getVersion().toString();
        
        if (id.endsWith(".feature.group")) {
            // Remove .feature.group suffix and add (feature) label
            String baseId = id.substring(0, id.length() - ".feature.group".length());
            return baseId + " " + version + " (feature)";
        }
        
        return id + " " + version;
    }
    
    /**
     * Checks if a unit has used children (excluding JRE units).
     */
    private boolean hasUsedChildrenExcludingJRE(IInstallableUnit unit, UsageReport report) {
        Set<IInstallableUnit> allChildren = report.getAllChildren(unit);
        return allChildren.stream()
                .filter(report.usedUnits::contains)
                .anyMatch(child -> !child.getId().equals(SYNTHETIC_JRE_UNIT_ID));
    }
    
    /**
     * Checks if the path from this unit to any used child contains another feature.
     */
    private boolean pathToUsedChildContainsFeature(IInstallableUnit unit, UsageReport report,
            Map<IInstallableUnit, List<IInstallableUnit>> shortestPaths) {
        Set<IInstallableUnit> allChildren = report.getAllChildren(unit);
        Set<IInstallableUnit> usedChildren = allChildren.stream()
                .filter(report.usedUnits::contains)
                .filter(child -> !child.getId().equals(SYNTHETIC_JRE_UNIT_ID))
                .collect(Collectors.toSet());
        
        for (IInstallableUnit usedChild : usedChildren) {
            List<IInstallableUnit> path = shortestPaths.get(usedChild);
            if (path != null && path.size() > 0 && path.get(0).equals(unit)) {
                // Check if any intermediate node in the path is a feature
                for (int i = 1; i < path.size() - 1; i++) {
                    if (path.get(i).getId().endsWith(".feature.group")) {
                        return true;
                    }
                }
            } else {
                // Fallback: compute path
                path = report.findPathBetween(unit, usedChild);
                for (int i = 1; i < path.size() - 1; i++) {
                    if (path.get(i).getId().endsWith(".feature.group")) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Displays a list of projects with optional limit.
     */
    private void displayProjectList(List<String> projects, Consumer<String> reportConsumer, String indent) {
        int maxProjects = 5;
        int displayCount = Math.min(projects.size(), maxProjects);
        
        for (int i = 0; i < displayCount; i++) {
            reportConsumer.accept(indent + "└─ " + projects.get(i));
        }
        
        if (projects.size() > maxProjects) {
            int remaining = projects.size() - maxProjects;
            reportConsumer.accept(indent + "└─ ... and " + remaining + " more ...");
        }
    }

    /**
     * Displays the indirect usage chain as a tree structure.
     */
    private void displayIndirectUsageTree(IInstallableUnit unit, UsageReport report, Consumer<String> reportConsumer,
            Set<IInstallableUnit> reportedUnits, Map<IInstallableUnit, List<IInstallableUnit>> shortestPaths, 
            boolean verbose) {
        Set<IInstallableUnit> allChildren = report.getAllChildren(unit);
        Set<IInstallableUnit> usedChildren = allChildren.stream()
                .filter(report.usedUnits::contains)
                // Filter out synthetic JRE units
                .filter(child -> !child.getId().equals(SYNTHETIC_JRE_UNIT_ID))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        
        if (usedChildren.isEmpty()) {
            return;
        }
        
        // Find the shortest path to any used child that hasn't been reported yet
        IInstallableUnit targetChild = null;
        List<IInstallableUnit> shortestPath = null;
        
        for (IInstallableUnit usedChild : usedChildren) {
            // Skip if already reported
            if (reportedUnits.contains(usedChild)) {
                continue;
            }
            
            // Use the pre-computed shortest path if available
            List<IInstallableUnit> path = shortestPaths.get(usedChild);
            if (path != null && path.size() > 0 && path.get(0).equals(unit)) {
                // This path starts from our unit
                if (shortestPath == null || path.size() < shortestPath.size()) {
                    shortestPath = path;
                    targetChild = usedChild;
                }
            } else {
                // Fallback: compute path
                path = report.findPathBetween(unit, usedChild);
                if (shortestPath == null || path.size() < shortestPath.size()) {
                    shortestPath = path;
                    targetChild = usedChild;
                }
            }
        }
        
        if (shortestPath == null || shortestPath.size() <= 1) {
            return;
        }
        
        // Display the shortest path as a tree (skip first element as it's the unit itself)
        for (int i = 1; i < shortestPath.size(); i++) {
            IInstallableUnit pathUnit = shortestPath.get(i);
            
            // Skip synthetic JRE units
            if (pathUnit.getId().equals(SYNTHETIC_JRE_UNIT_ID)) {
                continue;
            }
            
            boolean isLast = (i == shortestPath.size() - 1);
            
            // Create the tree connector with proper indentation
            StringBuilder indentBuilder = new StringBuilder("      ");
            for (int j = 1; j < i; j++) {
                indentBuilder.append("   ");
            }
            String indent = indentBuilder.toString();
            String connector = "└─";
            
            String line = indent + connector + " " + formatUnit(pathUnit);
            
            // If this is the last node and it's used, add project count
            if (isLast && report.usedUnits.contains(pathUnit)) {
                List<String> projects = report.projectUsage.entrySet().stream()
                        .filter(entry -> entry.getValue().contains(pathUnit))
                        .map(project -> project.getKey().getId()).toList();
                line += " (" + projects.size() + " project" + (projects.size() == 1 ? "" : "s") + ")";
                reportConsumer.accept(line);
                
                // If verbose, display the project IDs
                if (verbose) {
                    String projectIndent = indent + "   ";
                    displayProjectList(projects, reportConsumer, projectIndent);
                }
            } else {
                reportConsumer.accept(line);
            }
        }
    }

    /**
     * Builds a structure mapping target files to locations to units.
     */
    private Map<TargetDefinition, Map<String, List<UnitInfo>>> buildTargetStructure(UsageReport report) {
        Map<TargetDefinition, Map<String, List<UnitInfo>>> structure = new LinkedHashMap<>();

        // Iterate through all units and organize by target and location
        for (Map.Entry<IInstallableUnit, Set<UsageReport.TargetDefinitionLocationReference>> entry : report.providedBy
                .entrySet()) {
            IInstallableUnit unit = entry.getKey();

            for (UsageReport.TargetDefinitionLocationReference ref : entry.getValue()) {
                TargetDefinition targetFile = ref.file();
                String location = ref.location();

                Map<String, List<UnitInfo>> locations = structure.computeIfAbsent(targetFile,
                        k -> new LinkedHashMap<>());
                List<UnitInfo> units = locations.computeIfAbsent(location, k -> new ArrayList<>());

                // Only add root units (those with no parent in the dependency tree)
                if (ref.parent() == null) {
                    units.add(new UnitInfo(unit, ref.parent()));
                }
            }
        }

        return structure;
    }

    /**
     * Computes the shortest path from any root unit to each used unit.
     * This is used to deduplicate units that appear in multiple chains.
     */
    private Map<IInstallableUnit, List<IInstallableUnit>> computeShortestPaths(UsageReport report,
            Set<IInstallableUnit> rootUnits) {
        Map<IInstallableUnit, List<IInstallableUnit>> shortestPaths = new HashMap<>();

        for (IInstallableUnit usedUnit : report.usedUnits) {
            // Skip synthetic JRE units
            if (usedUnit.getId().equals(SYNTHETIC_JRE_UNIT_ID)) {
                continue;
            }

            // Find shortest path from any root to this used unit
            List<IInstallableUnit> shortestPath = null;

            for (IInstallableUnit rootUnit : rootUnits) {
                // Skip synthetic JRE units
                if (rootUnit.getId().equals(SYNTHETIC_JRE_UNIT_ID)) {
                    continue;
                }

                // Check if this root can reach the used unit
                Set<IInstallableUnit> children = report.getAllChildren(rootUnit);
                if (children.contains(usedUnit) || rootUnit.equals(usedUnit)) {
                    List<IInstallableUnit> path = report.findPathBetween(rootUnit, usedUnit);
                    if (shortestPath == null || path.size() < shortestPath.size()) {
                        shortestPath = path;
                    }
                }
            }

            if (shortestPath != null) {
                shortestPaths.put(usedUnit, shortestPath);
            }
        }

        return shortestPaths;
    }

    /**
     * Sorts locations by the number of unique projects using units from that location.
     * Locations with more projects are listed first.
     */
    private List<String> sortLocationsByProjectCount(Map<String, List<UnitInfo>> locations, UsageReport report) {
        Map<String, Integer> locationProjectCounts = new HashMap<>();

        for (Map.Entry<String, List<UnitInfo>> entry : locations.entrySet()) {
            String location = entry.getKey();
            List<UnitInfo> units = entry.getValue();

            Set<String> uniqueProjects = countUniqueProjects(units, report);
            locationProjectCounts.put(location, uniqueProjects.size());
        }

        // Sort by project count (descending), then by name
        return locationProjectCounts.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Counts unique projects that use any of the given units or their transitive dependencies.
     */
    private Set<String> countUniqueProjects(List<UnitInfo> units, UsageReport report) {
        Set<String> uniqueProjects = new HashSet<>();

        for (UnitInfo unitInfo : units) {
            IInstallableUnit unit = unitInfo.unit;

            // Skip synthetic JRE units
            if (unit.getId().equals(SYNTHETIC_JRE_UNIT_ID)) {
                continue;
            }

            // Add projects using this unit
            if (report.usedUnits.contains(unit)) {
                report.projectUsage.entrySet().stream()
                        .filter(entry -> entry.getValue().contains(unit))
                        .forEach(entry -> uniqueProjects.add(entry.getKey().getId()));
            }

            // Add projects using any transitive dependency
            Set<IInstallableUnit> children = report.getAllChildren(unit);
            for (IInstallableUnit child : children) {
                if (report.usedUnits.contains(child)) {
                    report.projectUsage.entrySet().stream()
                            .filter(entry -> entry.getValue().contains(child))
                            .forEach(entry -> uniqueProjects.add(entry.getKey().getId()));
                }
            }
        }

        return uniqueProjects;
    }

    /**
     * Sorts units by usage count (most used to least used, then unused).
     */
    private List<UnitInfo> sortUnitsByUsage(List<UnitInfo> units, UsageReport report) {
        return units.stream()
                .sorted((ui1, ui2) -> {
                    IInstallableUnit u1 = ui1.unit;
                    IInstallableUnit u2 = ui2.unit;

                    // Get usage counts
                    int count1 = getUsageCount(u1, report);
                    int count2 = getUsageCount(u2, report);

                    // Sort by count descending (most used first)
                    if (count1 != count2) {
                        return Integer.compare(count2, count1);
                    }

                    // If counts are equal, sort by unit name
                    return u1.toString().compareTo(u2.toString());
                })
                .toList();
    }

    /**
     * Gets the number of projects using this unit (directly or indirectly).
     */
    private int getUsageCount(IInstallableUnit unit, UsageReport report) {
        Set<String> projects = new HashSet<>();

        // Direct usage
        if (report.usedUnits.contains(unit)) {
            report.projectUsage.entrySet().stream()
                    .filter(entry -> entry.getValue().contains(unit))
                    .forEach(entry -> projects.add(entry.getKey().getId()));
        }

        // Indirect usage through children
        Set<IInstallableUnit> children = report.getAllChildren(unit);
        for (IInstallableUnit child : children) {
            if (report.usedUnits.contains(child)) {
                report.projectUsage.entrySet().stream()
                        .filter(entry -> entry.getValue().contains(child))
                        .forEach(entry -> projects.add(entry.getKey().getId()));
            }
        }

        return projects.size();
    }

    private static class UnitInfo {
        final IInstallableUnit unit;
        final IInstallableUnit parent;

        UnitInfo(IInstallableUnit unit, IInstallableUnit parent) {
            this.unit = unit;
            this.parent = parent;
        }
    }
}
