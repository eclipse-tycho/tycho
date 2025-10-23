package org.eclipse.tycho.extras.pde.usage;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.project.MavenProject;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.targetplatform.TargetDefinition;
import org.eclipse.tycho.targetplatform.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.Location;
import org.eclipse.tycho.targetplatform.TargetDefinition.TargetReferenceLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.Unit;
import org.eclipse.tycho.targetplatform.TargetDefinitionContent;

final class UsageReport {
    /**
     * Maximum number of indirect usage examples to show in the report
     */
    private static final int MAX_INDIRECT_USAGE_EXAMPLES = 3;

    /**
     * Maps a project to the units it uses (or at least is resolved to)
     */
    final Map<MavenProject, Set<IInstallableUnit>> projectUsage = new HashMap<>();
    /**
     * A collection of units used by all projects in the reactor
     */
    final Set<IInstallableUnit> usedUnits = new HashSet<>();
    /**
     * A collection of all used target definitions in the reactor
     */
    private final Set<TargetDefinition> targetFiles = new HashSet<>();

    /**
     * Maps a target definition to a list of other targets where it is referenced
     */
    final Map<TargetDefinition, List<TargetDefinition>> targetReferences = new HashMap<>();
    /**
     * Maps a target definition to its actual content
     */
    private final Map<TargetDefinition, TargetDefinitionContent> targetFileUnits = new HashMap<>();
    /**
     * Maps a unit to the set of definition files this unit is defined in
     */
    final Map<IInstallableUnit, Set<TargetDefinitionLocationReference>> providedBy = new HashMap<>();

    private final Map<IInstallableUnit, Set<IInstallableUnit>> parentMap = new HashMap<>();

    private final Map<IInstallableUnit, Set<IInstallableUnit>> childMap = new HashMap<>();

    private void reportProvided(IInstallableUnit iu, TargetDefinition file, String location, IInstallableUnit parent) {
        if (parent != null) {
            parentMap.computeIfAbsent(iu, nil -> new HashSet<>()).add(parent);
            childMap.computeIfAbsent(parent, nil -> new HashSet<>()).add(iu);
        }
        providedBy.computeIfAbsent(iu, nil -> new HashSet<>())
                .add(new TargetDefinitionLocationReference(parent, file, location));
    }

    static record TargetDefinitionLocationReference(IInstallableUnit parent, TargetDefinition file, String location) {

    }

    /**
     * Returns true if this unit is a root unit (defined directly in the target file)
     */
    boolean isRootUnit(IInstallableUnit unit) {
        Set<TargetDefinitionLocationReference> refs = providedBy.get(unit);
        if (refs == null) {
            return false;
        }
        return refs.stream().anyMatch(ref -> ref.parent() == null);
    }

    /**
     * Gets all transitive children of a unit
     */
    Set<IInstallableUnit> getAllChildren(IInstallableUnit unit) {
        Set<IInstallableUnit> result = new HashSet<>();
        collectChildren(unit, result);
        return result;
    }

    private void collectChildren(IInstallableUnit unit, Set<IInstallableUnit> result) {
        Set<IInstallableUnit> children = childMap.get(unit);
        if (children != null) {
            for (IInstallableUnit child : children) {
                if (result.add(child)) {
                    collectChildren(child, result);
                }
            }
        }
    }

    /**
     * Checks if any child in the dependency tree of this unit is used
     */
    boolean hasUsedChildren(IInstallableUnit unit) {
        Set<IInstallableUnit> allChildren = getAllChildren(unit);
        return allChildren.stream().anyMatch(usedUnits::contains);
    }

    /**
     * Gets the shortest path from a root unit to the target unit
     */
    List<IInstallableUnit> getShortestPathFromRoot(IInstallableUnit unit) {
        // BFS to find shortest path from any root to this unit
        Set<IInstallableUnit> visited = new HashSet<>();
        Deque<List<IInstallableUnit>> queue = new ArrayDeque<>();

        // Start from the unit itself
        List<IInstallableUnit> initialPath = new ArrayList<>();
        initialPath.add(unit);
        queue.add(initialPath);
        visited.add(unit);

        List<IInstallableUnit> shortestPath = null;

        while (!queue.isEmpty()) {
            List<IInstallableUnit> currentPath = queue.poll();
            IInstallableUnit current = currentPath.get(currentPath.size() - 1);

            // Check if we've reached a root
            if (isRootUnit(current)) {
                if (shortestPath == null || currentPath.size() < shortestPath.size()) {
                    shortestPath = new ArrayList<>(currentPath);
                }
                continue; // Keep searching for potentially shorter paths
            }

            // Explore parents
            Set<IInstallableUnit> parents = parentMap.get(current);
            if (parents != null) {
                for (IInstallableUnit parent : parents) {
                    if (!visited.contains(parent)) {
                        visited.add(parent);
                        List<IInstallableUnit> newPath = new ArrayList<>(currentPath);
                        newPath.add(parent);
                        queue.add(newPath);
                    }
                }
            }
        }

        // Reverse the path so it goes from root to unit
        if (shortestPath != null) {
            List<IInstallableUnit> reversed = new ArrayList<>(shortestPath);
            java.util.Collections.reverse(reversed);
            return reversed;
        }

        return List.of(unit);
    }

    /**
     * Returns formatted string showing where the unit is provided from, including nesting
     */
    String getProvidedBy(IInstallableUnit unit) {
        Set<TargetDefinitionLocationReference> refs = providedBy.get(unit);
        if (refs == null || refs.isEmpty()) {
            return "unknown";
        }

        // Find the reference with the shortest path to a root
        return refs.stream().map(ref -> {
            StringBuilder sb = new StringBuilder();
            sb.append(ref.file().getOrigin());
            sb.append(" > ");
            sb.append(ref.location());

            // If this has a parent, show the nesting
            if (ref.parent() != null) {
                List<IInstallableUnit> path = getShortestPathFromRoot(unit);
                if (path.size() > 1) {
                    sb.append(" > ");
                    sb.append(path.stream().map(IInstallableUnit::toString).collect(Collectors.joining(" > ")));
                }
            }

            return sb.toString();
        }).collect(Collectors.joining("; "));
    }

    /**
     * Checks if a unit is used indirectly (used unit is a descendant, not the unit itself)
     */
    boolean isUsedIndirectly(IInstallableUnit unit) {
        if (usedUnits.contains(unit)) {
            return false; // Used directly
        }
        return hasUsedChildren(unit);
    }

    /**
     * Gets the chain showing how this unit is indirectly used
     */
    String getIndirectUsageChain(IInstallableUnit unit) {
        Set<IInstallableUnit> allChildren = getAllChildren(unit);
        Set<IInstallableUnit> usedChildren = allChildren.stream().filter(usedUnits::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (usedChildren.isEmpty()) {
            return "";
        }

        // For each used child, find the shortest path from this unit to it
        return usedChildren.stream().limit(MAX_INDIRECT_USAGE_EXAMPLES).map(usedChild -> {
            List<IInstallableUnit> path = findPathBetween(unit, usedChild);
            return path.stream().map(IInstallableUnit::toString).collect(Collectors.joining(" > "));
        }).collect(Collectors.joining("; "));
    }

    /**
     * Finds shortest path between two units using BFS
     */
    List<IInstallableUnit> findPathBetween(IInstallableUnit start, IInstallableUnit end) {
        Set<IInstallableUnit> visited = new HashSet<>();
        Deque<List<IInstallableUnit>> queue = new ArrayDeque<>();

        List<IInstallableUnit> initialPath = new ArrayList<>();
        initialPath.add(start);
        queue.add(initialPath);
        visited.add(start);

        while (!queue.isEmpty()) {
            List<IInstallableUnit> currentPath = queue.poll();
            IInstallableUnit current = currentPath.get(currentPath.size() - 1);

            if (current.equals(end)) {
                return currentPath;
            }

            Set<IInstallableUnit> children = childMap.get(current);
            if (children != null) {
                for (IInstallableUnit child : children) {
                    if (!visited.contains(child)) {
                        visited.add(child);
                        List<IInstallableUnit> newPath = new ArrayList<>(currentPath);
                        newPath.add(child);
                        queue.add(newPath);
                    }
                }
            }
        }

        return List.of(start, end); // Fallback
    }

    void analyzeLocations(TargetDefinition definitionFile, TargetDefinitionResolver targetResolver,
            BiConsumer<Location, RuntimeException> exceptionConsumer) {
        if (targetFiles.add(definitionFile)) {
            targetFileUnits.put(definitionFile, targetResolver.fetchContent(definitionFile));
            for (Location location : definitionFile.getLocations()) {
                try {
                    if (location instanceof InstallableUnitLocation iu) {
                        analyzeIULocation(definitionFile, iu);
                    } else if (location instanceof TargetReferenceLocation ref) {
                        TargetDefinition referenceTargetDefinition = targetResolver
                                .getTargetDefinition(URI.create(ref.getUri()));
                        targetReferences.computeIfAbsent(referenceTargetDefinition, nil -> new ArrayList<>())
                                .add(definitionFile);
                        analyzeLocations(referenceTargetDefinition, targetResolver, exceptionConsumer);
                    }
                } catch (RuntimeException e) {
                    exceptionConsumer.accept(location, e);
                }
            }
        }
    }

    private void analyzeIULocation(TargetDefinition file, InstallableUnitLocation location) {
        List<? extends Unit> units = location.getUnits();
        String ref = location.getRepositories().stream().map(r -> r.getLocation()).collect(Collectors.joining(", "));
        TargetDefinitionContent content = targetFileUnits.get(file);
        for (Unit unit : units) {
            String id = unit.getId();
            String version = unit.getVersion();
            Optional<IInstallableUnit> found;
            if (version == null || version.isBlank() || version.equals("0.0.0")) {
                found = content.query(QueryUtil.createIUQuery(id), null).stream().findFirst();
            } else if (version.startsWith("[") || version.startsWith("(")) {
                found = content
                        .query(QueryUtil.createLatestQuery(QueryUtil.createIUQuery(id, VersionRange.create(version))),
                                null)
                        .stream().findFirst();
            } else {
                found = content.query(QueryUtil.createIUQuery(id, Version.create(version)), null).stream().findFirst();
            }
            if (found.isPresent()) {
                IInstallableUnit iu = found.get();
                reportUsage(iu, null, file, ref, content, new HashSet<>());
            }
        }
    }

    private void reportUsage(IInstallableUnit iu, IInstallableUnit parent, TargetDefinition file, String location,
            TargetDefinitionContent content, Set<IInstallableUnit> seen) {
        if (seen.add(iu)) {
            reportProvided(iu, file, location, parent);
            Collection<IRequirement> requirements = iu.getRequirements();
            Set<IInstallableUnit> units = content.query(QueryUtil.ALL_UNITS, null).toSet();
            for (IRequirement requirement : requirements) {
                for (IInstallableUnit provider : units) {
                    if (provider.satisfies(requirement)) {
                        reportUsage(provider, iu, file, location, content, seen);
                    }
                }
            }
        }
    }
    
    /**
     * Checks if any unit from a referenced target is used in any project.
     * 
     * @param refUri the URI of the referenced target
     * @return true if any unit from the referenced target is used, false otherwise
     */
    boolean isReferencedTargetUsed(String refUri) {
        // Find the target definition by URI
        // The refUri might be a full file:// URI or a relative path
        // We need to match it against the origin which might be just a filename
        for (TargetDefinition target : targetFiles) {
            String origin = target.getOrigin();
            
            // Check for exact match or if one ends with the other
            if (origin.equals(refUri) || 
                origin.endsWith(refUri) || 
                refUri.endsWith(origin) ||
                refUri.endsWith("/" + origin)) {
                
                // Check if any unit from this target is used
                Set<IInstallableUnit> targetUnits = providedBy.entrySet().stream()
                        .filter(entry -> entry.getValue().stream()
                                .anyMatch(ref -> ref.file().equals(target)))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
                
                // Check if any of these units (or their children) are used
                for (IInstallableUnit unit : targetUnits) {
                    if (usedUnits.contains(unit)) {
                        return true;
                    }
                    Set<IInstallableUnit> children = getAllChildren(unit);
                    if (children.stream().anyMatch(usedUnits::contains)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns a stream of all target files analyzed by this report.
     * 
     * @return stream of target definitions
     */
    Stream<TargetDefinition> getTargetFiles() {
        return targetFiles.stream();
    }

    /**
     * Returns the number of target files analyzed by this report.
     * 
     * @return the count of target files
     */
    int getTargetFilesCount() {
        return targetFiles.size();
    }

    /**
     * Returns the target definition content for the specified target definition.
     * 
     * @param targetDefinition the target definition to get content for
     * @return the target definition content, or null if not found
     */
    TargetDefinitionContent getTargetDefinitionContent(TargetDefinition targetDefinition) {
        return targetFileUnits.get(targetDefinition);
    }

}
