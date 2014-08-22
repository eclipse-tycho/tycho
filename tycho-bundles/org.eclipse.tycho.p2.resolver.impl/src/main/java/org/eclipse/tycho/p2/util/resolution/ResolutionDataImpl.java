/*******************************************************************************
 * Copyright (c) 2012, 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.util.resolution;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;

public class ResolutionDataImpl implements ResolutionData {

    private final ExecutionEnvironmentResolutionHints eeResolutionHints;

    private Collection<IInstallableUnit> availableIUs;
    private Collection<IInstallableUnit> rootIUs;
    private List<IRequirement> additionalRequirements;
    private Map<String, String> additionalFilterProperties;

    public ResolutionDataImpl(ExecutionEnvironmentResolutionHints eeResolutionHints) {
        this.eeResolutionHints = eeResolutionHints;
    }

    @Override
    public Collection<IInstallableUnit> getAvailableIUs() {
        return availableIUs;
    }

    public void setAvailableIUs(Collection<IInstallableUnit> availableIUs) {
        this.availableIUs = availableIUs;
    }

    @Override
    public Collection<IInstallableUnit> getRootIUs() {
        return rootIUs;
    }

    public void setRootIUs(Collection<IInstallableUnit> rootIUs) {
        this.rootIUs = rootIUs;
    }

    @Override
    public List<IRequirement> getAdditionalRequirements() {
        return additionalRequirements;
    }

    public void setAdditionalRequirements(List<IRequirement> additionalRequirements) {
        this.additionalRequirements = additionalRequirements;
    }

    @Override
    public ExecutionEnvironmentResolutionHints getEEResolutionHints() {
        return eeResolutionHints;
    }

    @Override
    public Map<String, String> getAdditionalFilterProperties() {
        if (additionalFilterProperties == null) {
            return Collections.emptyMap();
        }
        return additionalFilterProperties;
    }

    public void setAdditionalFilterProperties(Map<String, String> additionalFilterProperties) {
        this.additionalFilterProperties = additionalFilterProperties;
    }

}
