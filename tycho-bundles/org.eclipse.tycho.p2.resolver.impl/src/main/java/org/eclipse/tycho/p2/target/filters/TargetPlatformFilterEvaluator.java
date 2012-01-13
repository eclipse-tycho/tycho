/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target.filters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.artifacts.TargetPlatformFilter;
import org.eclipse.tycho.artifacts.TargetPlatformFilter.CapabilityPattern;
import org.eclipse.tycho.artifacts.TargetPlatformFilter.CapabilityType;
import org.eclipse.tycho.artifacts.TargetPlatformFilterSyntaxException;
import org.eclipse.tycho.core.facade.MavenLogger;

@SuppressWarnings("restriction")
public class TargetPlatformFilterEvaluator {

    private final List<TargetPlatformFilter> filters;
    final MavenLogger logger;
    private final FilterLogger filterLogger;

    public TargetPlatformFilterEvaluator(List<TargetPlatformFilter> filters, MavenLogger logger) {
        this.filters = Collections.unmodifiableList(new ArrayList<TargetPlatformFilter>(filters));
        this.logger = logger;

        // TODO enable debug logging; currently the filtering is done repeatedly on subsets of the target platform, leading to excessive amount of debug output 
//        if (this.logger.isDebugEnabled())
//            this.filterLogger = new DebugFilterLogger();
//        else
        this.filterLogger = new FilterLogger();

    }

    /**
     * Applies the filters to the given set. Prints out log messages.
     * 
     * TODO "filter" usually returns filtered results, consider different name
     * 
     * @param targetPlatformUnits
     *            The set of units to be filtered. Collection is modified by the method.
     */
    public void filterUnits(Collection<IInstallableUnit> targetPlatformUnits)
            throws TargetPlatformFilterSyntaxException {

        for (TargetPlatformFilter filter : filters) {
            applyFilter(filter, targetPlatformUnits);
        }
    }

    private void applyFilter(TargetPlatformFilter filter, Collection<IInstallableUnit> targetPlatformUnits) {
        switch (filter.getAction()) {
        case REMOVE_ALL:
            applyRemoveAllFilter(filter, targetPlatformUnits);
            return;

        case RESTRICT:
            applyRestrictionFilter(filter, targetPlatformUnits);
            return;
        }
    }

    private void applyRemoveAllFilter(TargetPlatformFilter filter, Collection<IInstallableUnit> targetPlatformUnits) {
        ParsedCapabilityPattern scopePattern = parsePattern(filter.getScopePattern(), null);

        // TODO implement debug logging

        for (Iterator<IInstallableUnit> unitIterator = targetPlatformUnits.iterator(); unitIterator.hasNext();) {
            IInstallableUnit unit = unitIterator.next();

            if (matches(unit, scopePattern)) {
                unitIterator.remove();
            }
        }
    }

    private void applyRestrictionFilter(TargetPlatformFilter filter, Collection<IInstallableUnit> targetPlatformUnits) {
        ParsedCapabilityPattern scopePattern = parsePattern(filter.getScopePattern(), null);
        ParsedCapabilityPattern restrictionPattern = parsePattern(filter.getActionPattern(), scopePattern);

        filterLogger.beginEvaluation(filter);

        for (Iterator<IInstallableUnit> unitIterator = targetPlatformUnits.iterator(); unitIterator.hasNext();) {
            IInstallableUnit unit = unitIterator.next();

            if (matches(unit, scopePattern)) {
                if (!matches(unit, restrictionPattern)) {
                    unitIterator.remove();

                    filterLogger.unitRemoved(unit);
                } else {
                    filterLogger.unitKept(unit);
                }
            }

        }

        filterLogger.endEvaluation();
    }

    private boolean matches(IInstallableUnit unit, ParsedCapabilityPattern pattern) {
        switch (pattern.getType()) {
        case P2_INSTALLABLE_UNIT:
            return pattern.matchesId(unit.getId()) && pattern.matchesVersion(unit.getVersion());

        case OSGI_BUNDLE:
            IProvidedCapability bundle = getBundleCapability(unit);
            if (bundle == null)
                return false;
            return pattern.matchesId(bundle.getName()) && pattern.matchesVersion(bundle.getVersion());

        case JAVA_PACKAGE:
            for (IProvidedCapability exportedPackage : getPackageCapabilities(unit)) {
                if (pattern.matchesId(exportedPackage.getName())
                        && pattern.matchesVersion(exportedPackage.getVersion())) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private IProvidedCapability getBundleCapability(IInstallableUnit unit) {
        for (IProvidedCapability capability : unit.getProvidedCapabilities()) {
            if (BundlesAction.CAPABILITY_NS_OSGI_BUNDLE.equals(capability.getNamespace())) {
                return capability;
            }
        }
        return null;
    }

    private List<IProvidedCapability> getPackageCapabilities(IInstallableUnit unit) {
        Collection<IProvidedCapability> allCapabilities = unit.getProvidedCapabilities();
        List<IProvidedCapability> packageCapabilities = new ArrayList<IProvidedCapability>(allCapabilities.size());

        for (IProvidedCapability capability : allCapabilities) {
            if (PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE.equals(capability.getNamespace())) {
                packageCapabilities.add(capability);
            }
        }
        return packageCapabilities;
    }

    /**
     * Wraps the given {@link CapabilityPattern} in a new instance with default values filled in and
     * typed version/version range attributes.
     * 
     * @param pattern
     *            The pattern to be wrapped.
     * @param defaultValues
     *            Values to substitute for the type and id attributes in case of <code>null</code>.
     *            Background: The action filter can inherit values from the scope filter to allow
     *            shorter notations.
     */
    private static ParsedCapabilityPattern parsePattern(CapabilityPattern pattern, ParsedCapabilityPattern defaultValues) {
        return new ParsedCapabilityPattern(pattern, defaultValues);
    }

    private static Version parseVersion(String version) {
        if (version == null) {
            return null;
        }
        try {
            return Version.parseVersion(version);
        } catch (IllegalArgumentException e) {
            throw new TargetPlatformFilterSyntaxException("Failed to parse version: " + version, e);
        }
    }

    private static VersionRange parseVersionRange(String versionRange) {
        if (versionRange == null) {
            return null;
        }
        try {
            return new VersionRange(versionRange);
        } catch (IllegalArgumentException e) {
            throw new TargetPlatformFilterSyntaxException("Failed to parse version range: " + versionRange, e);
        }
    }

    static class ParsedCapabilityPattern {

        private CapabilityType type;
        private String idRequirement;
        private Version versionReq;
        private VersionRange versionRangeReq;

        ParsedCapabilityPattern(CapabilityPattern pattern, ParsedCapabilityPattern defaultValues) {
            this.type = pattern.getType();
            this.idRequirement = pattern.getId();
            this.versionReq = parseVersion(pattern.getVersion());
            this.versionRangeReq = parseVersionRange(pattern.getVersionRange());

            if (defaultValues != null) {
                // apply default values in order [type, id] until the first explicit value is set
                if (this.type == null) {
                    this.type = defaultValues.type;
                    if (this.idRequirement == null) {
                        this.idRequirement = defaultValues.idRequirement;

                        // no need to inherit version requirements because these would always match: the action pattern only tests units which already matched the scope pattern
                    }
                }
            }
        }

        CapabilityType getType() {
            return type;
        }

        boolean matchesId(String id) {
            if (idRequirement == null)
                return true;
            return idRequirement.equals(id);
        }

        boolean matchesVersion(Version version) {
            return exactVersionPatternMatches(version) && versionRangePatternMatches(version);
        }

        private boolean exactVersionPatternMatches(Version version) {
            if (versionReq == null)
                return true;
            return versionReq.equals(version);
        }

        private boolean versionRangePatternMatches(Version version) {
            if (versionRangeReq == null)
                return true;
            return versionRangeReq.isIncluded(version);
        }
    }

    private class FilterLogger {
        TargetPlatformFilter currentFilter;
        int unitsKept;
        int unitsRemoved;

        public void beginEvaluation(TargetPlatformFilter filter) {
            currentFilter = filter;
            unitsKept = 0;
            unitsRemoved = 0;
        }

        public void unitKept(IInstallableUnit unit) {
            ++unitsKept;
        }

        public void unitRemoved(IInstallableUnit unit) {
            ++unitsRemoved;
        }

        public void endEvaluation() {
            if (unitsRemoved > 0 && unitsKept == 0) {
                logger.warn("Removed all units from the target platform matching {"
                        + currentFilter.getScopePattern().printMembers()
                        + "} because none of the units passed the restriction filter {"
                        + currentFilter.getActionPattern().printMembers() + "}");
            }
        }
    }

    private class DebugFilterLogger extends FilterLogger {
        @Override
        public void beginEvaluation(TargetPlatformFilter filter) {
            super.beginEvaluation(filter);
            logger.debug("Applying " + filter);
        }

        @Override
        public void unitKept(IInstallableUnit unit) {
            super.unitKept(unit);
            logger.debug("  Keeping unit " + unit.getId() + "/" + unit.getVersion());
        }

        @Override
        public void unitRemoved(IInstallableUnit unit) {
            super.unitRemoved(unit);
            logger.debug("  Removing unit " + unit.getId() + "/" + unit.getVersion());
        }
    }
}
