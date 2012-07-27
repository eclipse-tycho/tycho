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

/**
 * Settings for supporting resolution for a dedicated execution environment. Handles p2's "a.jre"
 * installable units representing the capabilities of the JRE or custom profiles.
 */
public interface ExecutionEnvironmentResolutionHints {

    /**
     * Returns <code>true</code> if an installable unit shall be removed from the available IUs.
     * This prevents that "a.jre" IUs for the wrong execution environment are used for resolution.
     */
    boolean isNonApplicableEEUnit(IInstallableUnit iu);

    /**
     * Returns the list of installable units that shall be added to the list of available IUs. This
     * ensures that all requirements for capabilities of the execution environment can be resolved.
     */
    Collection<IInstallableUnit> getAdditionalUnits();

    /**
     * Returns the list of installable units that shall be temporarily added to the list of
     * installable units, i.e. they shall be available during resolution but must be removed from
     * the resolution result.
     */
    Collection<IInstallableUnit> getTemporaryAdditions();

    /**
     * Returns additional requirements to execution environment units for the resolution, which
     * ensure that these required units become part of the resolution result.
     */
    // TODO for custom profiles, these need to be requirements, not units
    Collection<IInstallableUnit> getAdditionalRequires();

}
