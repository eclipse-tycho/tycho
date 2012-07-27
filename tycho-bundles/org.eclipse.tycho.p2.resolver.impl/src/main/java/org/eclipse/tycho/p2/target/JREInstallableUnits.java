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
package org.eclipse.tycho.p2.target;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.actions.JREAction;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.p2.resolver.ExecutionEnvironmentResolutionHints;

/**
 * Resolution hints for a standard execution environment, e.g. "CDC-1.0/Foundation-1.0" or
 * "JavaSE-1.7"
 */
@SuppressWarnings("restriction")
final class JREInstallableUnits implements ExecutionEnvironmentResolutionHints {

    private final String executionEnvironment;

    public JREInstallableUnits(String executionEnvironment) {
        this.executionEnvironment = executionEnvironment;
    }

    /**
     * p2 repositories are polluted with useless a.jre/config.a.jre IUs. These IUs do not represent
     * current/desired JRE and can expose resolver to packages that are not actually available.
     */
    public boolean isNonApplicableEEUnit(IInstallableUnit iu) {
        // See JREAction
        return iu.getId().startsWith("a.jre") || iu.getId().startsWith("config.a.jre");
    }

    /**
     * Return IUs that represent packages provided by target JRE
     * 
     * @param executionEnvironment
     */
    public Collection<IInstallableUnit> getAdditionalUnits() {
        Map<VersionedId, IInstallableUnit> units = new LinkedHashMap<VersionedId, IInstallableUnit>();

        // Some notable installable units, like org.eclipse.sdk.ide, have hard dependency on the garbage JRE IUs.
        // We provide those IUs as empty shells, i.e. without any provided capabilities.
        // This way these garbage IUs are present but are not interfering with dependency resolution.

        // add garbage JRE IUs first, so that are replaced with real ones
        put(units, newIU("a.jre", Version.create("1.6.0")));
        put(units, newIU("a.jre.javase", Version.create("1.6.0")));
        put(units, newIU("config.a.jre.javase", Version.create("1.6.0")));

        // generate real IUs that represent requested execution environment
        PublisherResult results = new PublisherResult();
        new JREAction(executionEnvironment).perform(new PublisherInfo(), results, null);
        results.query(QueryUtil.ALL_UNITS, null);
        Iterator<IInstallableUnit> iterator = results.query(QueryUtil.ALL_UNITS, null).iterator();
        while (iterator.hasNext()) {
            put(units, iterator.next());
        }

        return units.values();
        // TODO cache the result?
    }

    public Collection<IInstallableUnit> getAdditionalRequires() {
        return getAdditionalUnits();
    }

    public Collection<IInstallableUnit> getTemporaryAdditions() {
        // TODO 384494 only return the empty a.jre.javase/1.6.0 IUs here (if there is no real a.jre.javase/1.6.0 IU)
        return getAdditionalUnits();
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
        if (!(obj instanceof JREInstallableUnits))
            return false;
        JREInstallableUnits other = (JREInstallableUnits) obj;
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
