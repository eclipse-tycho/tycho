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
package org.eclipse.tycho.p2.resolver;

import java.util.Collection;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;

/**
 * Settings for supporting resolution for a dedicated execution environment. Handles p2's "a.jre"
 * installable units representing the capabilities of the JRE or custom profiles.
 * 
 * Types implementing this interface shall be immutable and provide reasonable implementations for
 * {@link #equals(Object)} and {@link #hashCode()}.
 */
public interface ExecutionEnvironmentResolutionHints {

    /**
     * Returns <code>true</code> if an installable unit shall be removed from the available IUs.
     * This prevents that "a.jre" IUs for the wrong execution environment are used for resolution.
     */
    boolean isNonApplicableEEUnit(IInstallableUnit iu);

    /**
     * Returns the list of installable units that shall be used during resolution. These units are
     * added to the available units so that requirements of the capabilities of the execution
     * environment can be resolved, and their use during resolution is enforced so that other units
     * providing the same capabilities are not used (unless they are needed for other reasons).
     */
    Collection<IInstallableUnit> getMandatoryUnits();

    /**
     * Returns requirements to execution environment units to ensure that a) the execution
     * environment units are available, and b) the units are used to the resolution result.
     */
    Collection<IRequirement> getMandatoryRequires();

    /**
     * Returns the list of installable units that shall be temporarily added to the list of
     * installable units, i.e. they shall be available during resolution but must be removed from
     * the resolution result.
     */
    Collection<IInstallableUnit> getTemporaryAdditions();

    @Override
    public boolean equals(Object obj);

    @Override
    public int hashCode();
}
