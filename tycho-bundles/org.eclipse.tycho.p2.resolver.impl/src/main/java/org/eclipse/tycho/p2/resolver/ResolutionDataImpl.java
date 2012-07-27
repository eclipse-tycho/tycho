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

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;

public class ResolutionDataImpl implements ResolutionData {

    private Collection<IInstallableUnit> availableIUs;
    private Collection<IInstallableUnit> rootIUs;
    private List<IRequirement> additionalRequirements;
    private ExecutionEnvironmentResolutionHints eeResolutionHints;

    public Collection<IInstallableUnit> getAvailableIUs() {
        return availableIUs;
    }

    public void setAvailableIUs(Collection<IInstallableUnit> availableIUs) {
        this.availableIUs = availableIUs;
    }

    public Collection<IInstallableUnit> getRootIUs() {
        return rootIUs;
    }

    public void setRootIUs(Collection<IInstallableUnit> rootIUs) {
        this.rootIUs = rootIUs;
    }

    public List<IRequirement> getAdditionalRequirements() {
        return additionalRequirements;
    }

    public void setAdditionalRequirements(List<IRequirement> additionalRequirements) {
        this.additionalRequirements = additionalRequirements;
    }

    public ExecutionEnvironmentResolutionHints getEEResolutionHints() {
        return eeResolutionHints;
    }

    public void setEEResolutionHints(ExecutionEnvironmentResolutionHints eeResolutionHints) {
        this.eeResolutionHints = eeResolutionHints;
    }

}
