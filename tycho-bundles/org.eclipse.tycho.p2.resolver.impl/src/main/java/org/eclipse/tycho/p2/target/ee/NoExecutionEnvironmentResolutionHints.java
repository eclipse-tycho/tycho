/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tycho.p2.target.ee;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.tycho.p2.util.resolution.ExecutionEnvironmentResolutionHints;

public class NoExecutionEnvironmentResolutionHints implements ExecutionEnvironmentResolutionHints {

    public static final NoExecutionEnvironmentResolutionHints INSTANCE = new NoExecutionEnvironmentResolutionHints();

    private NoExecutionEnvironmentResolutionHints() {
    }

    @Override
    public boolean isEESpecificationUnit(IInstallableUnit unit) {
        return false;
    }

    @Override
    public boolean isNonApplicableEEUnit(IInstallableUnit iu) {
        return false;
    }

    @Override
    public Collection<IInstallableUnit> getMandatoryUnits() {
        return Collections.emptySet();
    }

    @Override
    public Collection<IRequirement> getMandatoryRequires() {
        return Collections.emptySet();
    }

    @Override
    public Collection<IInstallableUnit> getTemporaryAdditions() {
        return Collections.emptySet();
    }

}
