/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target.ee;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.actions.JREAction;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.p2.util.resolution.ExecutionEnvironmentResolutionHints;

/**
 * Resolution hints for a standard execution environment, e.g. "CDC-1.0/Foundation-1.0" or
 * "JavaSE-1.7"
 */
@SuppressWarnings("restriction")
public final class StandardEEResolutionHints implements ExecutionEnvironmentResolutionHints {

    private static final String JRE_ACTION_FALLBACK_EE = "JavaSE-1.6";
    private static final Version JRE_ACTION_FALLBACK_VERSION = Version.parseVersion("1.6.0");

    private final String executionEnvironment;
    private final Map<VersionedId, IInstallableUnit> additionalUnits;
    private final Map<VersionedId, IInstallableUnit> temporaryUnits;

    public StandardEEResolutionHints(String executionEnvironment) {
        if (executionEnvironment == null) {
            // don't specify a default here; ExecutionEnvironmentConfiguration does the defaulting
            throw new NullPointerException();
        }

        this.executionEnvironment = executionEnvironment;
        this.additionalUnits = computeAdditionalUnits(executionEnvironment);
        this.temporaryUnits = computeTemporaryAdditions(additionalUnits);
    }

    /**
     * p2 repositories are polluted with useless a.jre/config.a.jre IUs. These IUs do not represent
     * current/desired JRE and can expose resolver to packages that are not actually available.
     */
    @Override
    public boolean isNonApplicableEEUnit(IInstallableUnit iu) {
        // See JREAction
        return iu.getId().startsWith("a.jre") || iu.getId().startsWith("config.a.jre");
    }

    @Override
    public boolean isEESpecificationUnit(IInstallableUnit unit) {
        // not needed
        throw new UnsupportedOperationException();
    }

    /**
     * Return IUs that represent packages provided by target JRE
     * 
     * @param executionEnvironment
     */
    private static Map<VersionedId, IInstallableUnit> computeAdditionalUnits(String executionEnvironment) {
        Map<VersionedId, IInstallableUnit> units = new LinkedHashMap<VersionedId, IInstallableUnit>();
        addIUsFromEnvironment(executionEnvironment, units);
        ensureEEWasKnownToJREAction(executionEnvironment, units.values());
        return units;
    }

    static void addIUsFromEnvironment(String executionEnvironment, Map<VersionedId, IInstallableUnit> units) {
        // generate real IUs that represent requested execution environment
        PublisherResult results = new PublisherResult();
        new JREAction(executionEnvironment).perform(new PublisherInfo(), results, null);
        results.query(QueryUtil.ALL_UNITS, null);
        Iterator<IInstallableUnit> iterator = results.query(QueryUtil.ALL_UNITS, null).iterator();
        while (iterator.hasNext()) {
            put(units, iterator.next());
        }
    }

    private static void ensureEEWasKnownToJREAction(String executionEnvironment, Collection<IInstallableUnit> eeUnits) {
        for (IInstallableUnit unit : eeUnits) {
            if (JRE_ACTION_FALLBACK_VERSION.equals(unit.getVersion())
                    && !JRE_ACTION_FALLBACK_EE.equals(executionEnvironment)) {
                // the JREAction didn't actually recognize the EE but fell back to JavaSE-1.6 - and this although the EE was recognized as standard EE before -> internal error 
                throw new RuntimeException("The execution environment '" + executionEnvironment
                        + "' is not know by the embedded version of p2");
            }
        }
    }

    @Override
    public Collection<IInstallableUnit> getMandatoryUnits() {
        return additionalUnits.values();
    }

    @Override
    public Collection<IRequirement> getMandatoryRequires() {
        // not needed; getMandatoryUnits already enforces the use of the JRE IUs during resolution
        return Collections.emptyList();
    }

    private static Map<VersionedId, IInstallableUnit> computeTemporaryAdditions(
            Map<VersionedId, IInstallableUnit> additionalUnits) {
        Map<VersionedId, IInstallableUnit> units = new LinkedHashMap<VersionedId, IInstallableUnit>();

        // Some notable installable units, like org.eclipse.sdk.ide, have hard dependency on the garbage JRE IUs.
        // We provide those IUs as empty shells, i.e. without any provided capabilities.
        // This way these garbage IUs are present but are not interfering with dependency resolution.

        put(units, newIU("a.jre", Version.create("1.6.0")));
        put(units, newIU("a.jre.javase", Version.create("1.6.0")));
        put(units, newIU("config.a.jre.javase", Version.create("1.6.0")));

        // don't override real units
        for (Entry<VersionedId, IInstallableUnit> entry : additionalUnits.entrySet()) {
            units.remove(entry.getKey());
        }

        return units;
    }

    @Override
    public Collection<IInstallableUnit> getTemporaryAdditions() {
        return temporaryUnits.values();
    }

    private static IInstallableUnit newIU(String id, Version version) {
        InstallableUnitDescription iud = new InstallableUnitDescription();
        iud.setId(id);
        iud.setVersion(version);
        iud.addProvidedCapabilities(Collections.singleton(MetadataFactory.createProvidedCapability(
                IInstallableUnit.NAMESPACE_IU_ID, id, version)));
        return MetadataFactory.createInstallableUnit(iud);
    }

    private static void put(Map<VersionedId, IInstallableUnit> units, IInstallableUnit unit) {
        units.put(new VersionedId(unit.getId(), unit.getVersion()), unit);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((executionEnvironment == null) ? 0 : executionEnvironment.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof StandardEEResolutionHints))
            return false;
        StandardEEResolutionHints other = (StandardEEResolutionHints) obj;
        return eq(executionEnvironment, other.executionEnvironment);
    }

    private static <T> boolean eq(T left, T right) {
        if (left == right) {
            return true;
        } else if (left == null) {
            return false;
        } else {
            return left.equals(right);
        }
    }
}
