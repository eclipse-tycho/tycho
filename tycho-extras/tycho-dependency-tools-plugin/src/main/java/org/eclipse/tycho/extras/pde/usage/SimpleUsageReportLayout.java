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

import java.util.HashSet;
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
 * Simple one-line-per-unit report layout (original format).
 */
@Component(role = ReportLayout.class, hint = "simple")
final class SimpleUsageReportLayout implements ReportLayout {

    @Override
    public void generateReport(UsageReport report, boolean verbose, Consumer<String> reportConsumer) {
        reportConsumer.accept("###### DEPENDENCIES USAGE REPORT #######");
        reportConsumer.accept("Your build uses " + report.usedUnits.size() + " dependencies.");
        reportConsumer.accept("Your build uses " + report.getTargetFilesCount() + " target file(s).");
        report.getTargetFiles().forEach(targetFile -> {
            reportConsumer.accept(targetFile.getOrigin() + " contains "
                    + report.getTargetDefinitionContent(targetFile).query(QueryUtil.ALL_UNITS, null).toSet().size()
                    + " units from " + targetFile.getLocations().size() + " locations");
            
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
        });

        // Only report on root units (defined directly in target files)
        Set<IInstallableUnit> allUnits = report.providedBy.keySet();
        Set<IInstallableUnit> rootUnits = allUnits.stream().filter(report::isRootUnit).collect(Collectors.toSet());

        // Track which units have been covered by reporting their parent
        Set<IInstallableUnit> reportedUnits = new HashSet<>();

        for (IInstallableUnit unit : rootUnits) {
            // Skip if this unit was already covered by a parent report
            if (reportedUnits.contains(unit)) {
                continue;
            }

            String by = report.getProvidedBy(unit);

            if (report.usedUnits.contains(unit)) {
                // Unit is directly used - report it and mark all its children as reported
                List<String> list = report.projectUsage.entrySet().stream()
                        .filter(entry -> entry.getValue().contains(unit)).map(project -> project.getKey().getId())
                        .toList();
                reportConsumer.accept("The unit " + unit + " is used by " + list.size()
                        + " projects and currently provided by " + by);

                // Mark this unit and all its transitive dependencies as reported
                reportedUnits.add(unit);
                reportedUnits.addAll(report.getAllChildren(unit));
            } else if (report.isUsedIndirectly(unit)) {
                // Unit is indirectly used (one of its dependencies is used but not the unit itself)
                String chain = report.getIndirectUsageChain(unit);
                reportConsumer.accept("The unit " + unit + " is INDIRECTLY used through: " + chain
                        + " and currently provided by " + by);

                // Mark this unit and all its transitive dependencies as reported
                reportedUnits.add(unit);
                reportedUnits.addAll(report.getAllChildren(unit));
            } else {
                // Unit and all its dependencies are unused
                reportConsumer.accept("The unit " + unit + " is UNUSED and currently provided by " + by);

                // Mark this unit and all its transitive dependencies as reported
                // (so we don't report them separately as unused)
                reportedUnits.add(unit);
                reportedUnits.addAll(report.getAllChildren(unit));
            }
        }
    }
}
