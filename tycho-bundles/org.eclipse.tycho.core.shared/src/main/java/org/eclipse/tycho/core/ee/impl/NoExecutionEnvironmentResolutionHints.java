/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.core.ee.impl;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.tycho.ExecutionEnvironmentResolutionHints;

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
