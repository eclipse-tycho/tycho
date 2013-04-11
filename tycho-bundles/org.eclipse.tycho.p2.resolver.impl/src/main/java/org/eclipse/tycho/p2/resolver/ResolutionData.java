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
