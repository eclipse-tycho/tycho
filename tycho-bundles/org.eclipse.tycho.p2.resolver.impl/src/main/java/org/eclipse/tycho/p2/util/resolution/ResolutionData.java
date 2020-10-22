/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.util.resolution;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;

public interface ResolutionData {

    Collection<IInstallableUnit> getAvailableIUs();

    Collection<IInstallableUnit> getRootIUs();

    List<IRequirement> getAdditionalRequirements();

    ExecutionEnvironmentResolutionHints getEEResolutionHints();

    Map<String, String> getAdditionalFilterProperties();
}
