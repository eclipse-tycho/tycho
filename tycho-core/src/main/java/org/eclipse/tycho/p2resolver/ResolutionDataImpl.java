/*******************************************************************************
 * Copyright (c) 2012, 2022 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - #462 - Delay Pom considered items to the final Target Platform calculation
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.ExecutionEnvironmentResolutionHints;

public class ResolutionDataImpl implements ResolutionData {

    private final ExecutionEnvironmentResolutionHints eeResolutionHints;

    private Collection<IInstallableUnit> availableIUs;
    private Collection<IInstallableUnit> rootIUs;
    private List<IRequirement> additionalRequirements;
    private Map<String, String> additionalFilterProperties;
    private Collection<IRequirement> missing = new ArrayList<>();
    private boolean failOnMissing = true;

    private Predicate<IInstallableUnit> slicerPredicate;

    private IQueryable<IInstallableUnit> additionalUnitStore;

    public ResolutionDataImpl(ExecutionEnvironmentResolutionHints eeResolutionHints) {
        this.eeResolutionHints = eeResolutionHints;
    }

    @Override
    public Collection<IInstallableUnit> getAvailableIUs() {
        return availableIUs;
    }

    /**
     * Sets the installable units which may be used by the resolver.
     * 
     * @param availableIUs
     *            the units available to the resolver. Must not contain any non-applicable execution
     *            environment units.
     */
    public void setAvailableIUs(Collection<IInstallableUnit> availableIUs) {
        this.availableIUs = availableIUs;
    }

    /**
     * Sets the available installable units, removing all non-applicable execution environment
     * units.
     * 
     * @see #setAvailableIUs(Collection)
     */
    public void setAvailableIUsAndFilter(IQueryable<IInstallableUnit> unfilteredAvailableUnits) {
        this.availableIUs = new LinkedHashSet<>();

        IQueryResult<IInstallableUnit> allUnits = unfilteredAvailableUnits.query(QueryUtil.ALL_UNITS,
                new NullProgressMonitor());
        copyApplyingEEFilter(allUnits.iterator(), this.availableIUs, eeResolutionHints);
    }

    private static void copyApplyingEEFilter(Iterator<IInstallableUnit> source, Collection<IInstallableUnit> sink,
            ExecutionEnvironmentResolutionHints eeResolutionHints) {
        while (source.hasNext()) {
            IInstallableUnit unit = source.next();
            if (!eeResolutionHints.isNonApplicableEEUnit(unit)) {
                sink.add(unit);
            }
        }
    }

    @Override
    public Collection<IInstallableUnit> getRootIUs() {
        return rootIUs;
    }

    public void setRootIUs(Collection<IInstallableUnit> rootIUs) {
        this.rootIUs = rootIUs;
    }

    @Override
    public List<IRequirement> getAdditionalRequirements() {
        return additionalRequirements;
    }

    public void setAdditionalRequirements(List<IRequirement> additionalRequirements) {
        this.additionalRequirements = additionalRequirements;
    }

    @Override
    public ExecutionEnvironmentResolutionHints getEEResolutionHints() {
        return eeResolutionHints;
    }

    @Override
    public Map<String, String> getAdditionalFilterProperties() {
        if (additionalFilterProperties == null) {
            return Collections.emptyMap();
        }
        return additionalFilterProperties;
    }

    public void setAdditionalFilterProperties(Map<String, String> additionalFilterProperties) {
        this.additionalFilterProperties = additionalFilterProperties;
    }

    @Override
    public boolean failOnMissingRequirements() {
        return failOnMissing;
    }

    public void setFailOnMissing(boolean failOnMissing) {
        this.failOnMissing = failOnMissing;
    }

    @Override
    public void addMissingRequirement(IRequirement requirement) {
        missing.add(requirement);
    }

    @Override
    public Collection<IRequirement> getMissingRequirements() {
        return Collections.unmodifiableCollection(missing);
    }

    public void clearMissingRequirements() {
        missing.clear();
    }

    @Override
    public Predicate<IInstallableUnit> getIInstallableUnitAcceptor() {
        return slicerPredicate;
    }

    public void setSlicerPredicate(Predicate<IInstallableUnit> slicerPredicate) {
        this.slicerPredicate = slicerPredicate;
    }

    @Override
    public IQueryable<IInstallableUnit> getAdditionalUnitStore() {
        return additionalUnitStore;
    }

    public void setAdditionalUnitStore(IQueryable<IInstallableUnit> additionalUnitStore) {
        this.additionalUnitStore = additionalUnitStore;
    }

}
