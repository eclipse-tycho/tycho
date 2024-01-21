/*******************************************************************************
 * Copyright (c) 2012, 2022 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Christoph Läubrich - #462 - Delay Pom considered items to the final Target Platform calculation
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.query.CollectionResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.tycho.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.p2maven.ListQueryable;

public interface ResolutionData {

    Collection<IInstallableUnit> getAvailableIUs();

    Collection<IInstallableUnit> getRootIUs();

    List<IRequirement> getAdditionalRequirements();

    ExecutionEnvironmentResolutionHints getEEResolutionHints();

    Map<String, String> getAdditionalFilterProperties();

    /**
     * 
     * @return a predicate that us used to check if a given unit should be accepted by the slicer
     */
    Predicate<IInstallableUnit> getIInstallableUnitAcceptor();

    IQueryable<IInstallableUnit> getAdditionalUnitStore();

    default IQueryable<IInstallableUnit> units() {
        ListQueryable<IInstallableUnit> listQueryable = new ListQueryable<>();
        listQueryable.add(new CollectionResult<>(getAvailableIUs()));
        IQueryable<IInstallableUnit> unitStore = getAdditionalUnitStore();
        if (unitStore != null) {
            listQueryable.add(unitStore);
        }
        return listQueryable;
    }
}
