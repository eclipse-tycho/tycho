/*******************************************************************************
 * Copyright (c) 2012 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.ee.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.publisher.actions.JREAction;
import org.eclipse.tycho.ExecutionEnvironmentResolutionHints;

public final class CustomEEResolutionHints implements ExecutionEnvironmentResolutionHints {

    // primary members
    private final String eeName;

    // derived members
    private transient String unitName;
    private transient Version unitVersion;

    public CustomEEResolutionHints(String eeName) throws InvalidEENameException {
        this.eeName = eeName;
        parse(eeName);
    }

    /** see {@link JREAction#generateJREIUData()} */
    void parse(String eeName) throws InvalidEENameException {
        int idx = eeName.indexOf('-');
        if (idx == -1) {
            throw new InvalidEENameException(eeName);
        }
        String name = eeName.substring(0, idx);
        name = name.replace('/', '.');
        name = name.replace('_', '.');
        this.unitName = "a.jre." + name.toLowerCase(Locale.ENGLISH);
        String version = eeName.substring(idx + 1);
        try {
            this.unitVersion = Version.create(version);
        } catch (IllegalArgumentException e) {
            throw new InvalidEENameException(eeName);
        }
    }

    @Override
    public boolean isEESpecificationUnit(IInstallableUnit unit) {
        return unitName.equals(unit.getId()) && unit.getVersion().equals(unitVersion);
    }

    @Override
    public boolean isNonApplicableEEUnit(IInstallableUnit iu) {
        return ExecutionEnvironmentResolutionHints.isJreUnit(iu) && !isEESpecificationUnit(iu);
    }

    @Override
    public Collection<IInstallableUnit> getMandatoryUnits() {
        return Collections.emptyList();
    }

    @Override
    public Collection<IInstallableUnit> getTemporaryAdditions() {
        return Collections.emptyList();
    }

    @Override
    public Collection<IRequirement> getMandatoryRequires() {
        VersionRange strictUnitRange = new VersionRange(unitVersion, true, unitVersion, true);
        return Collections.singleton(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, unitName,
                strictUnitRange, null, false, false));
    }

    @Override
    public int hashCode() {
        return Objects.hash(eeName);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || //
                (obj instanceof CustomEEResolutionHints other && Objects.equals(eeName, other.eeName));
    }
}
