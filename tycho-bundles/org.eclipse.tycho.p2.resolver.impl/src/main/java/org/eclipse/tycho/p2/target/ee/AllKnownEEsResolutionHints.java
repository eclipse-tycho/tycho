/*******************************************************************************
 * Copyright (c) 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target.ee;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.eclipse.tycho.p2.util.resolution.ExecutionEnvironmentResolutionHints;

public class AllKnownEEsResolutionHints implements ExecutionEnvironmentResolutionHints {

    private final Map<VersionedId, IInstallableUnit> temporaryUnits;

    public AllKnownEEsResolutionHints(Collection<ExecutionEnvironment> allKnownEEs) {
        temporaryUnits = new LinkedHashMap<>();
        for (ExecutionEnvironment ee : allKnownEEs) {
            StandardEEResolutionHints.addIUsFromEnvironment(ee, temporaryUnits);
        }
    }

    @Override
    public Collection<IInstallableUnit> getMandatoryUnits() {
        return Collections.emptyList();
    }

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

    @Override
    public Collection<IRequirement> getMandatoryRequires() {
        // not needed; getMandatoryUnits already enforces the use of the JRE IUs during resolution
        return Collections.emptyList();
    }

    @Override
    public Collection<IInstallableUnit> getTemporaryAdditions() {
        return temporaryUnits.values();
    }

}
