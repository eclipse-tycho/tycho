/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.tycho.p2.resolver.ExecutionEnvironmentResolutionHints;

public class NoopEEResolverHints implements ExecutionEnvironmentResolutionHints {

    public boolean isNonApplicableEEUnit(IInstallableUnit iu) {
        // don't remove anything
        return false;
    }

    public Collection<IInstallableUnit> getMandatoryUnits() {
        return Collections.emptyList();
    }

    public Collection<IInstallableUnit> getTemporaryAdditions() {
        return Collections.emptyList();
    }

    public Collection<IRequirement> getMandatoryRequires() {
        return Collections.emptyList();
    }

}
