/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.resolver;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.query.IQueryable;

public abstract class ResolutionStrategy {
    protected IQueryable<IInstallableUnit> availableIUs;

    protected Collection<IInstallableUnit> jreIUs;

    protected Set<IInstallableUnit> rootIUs;

    protected List<IRequirement> additionalRequirements;

    public void setAvailableInstallableUnits(IQueryable<IInstallableUnit> availableIUs) {
        this.availableIUs = availableIUs;
    }

    public void setRootInstallableUnits(Set<IInstallableUnit> rootIUs) {
        this.rootIUs = rootIUs;
    }

    public void setAdditionalRequirements(List<IRequirement> additionalRequirements) {
        this.additionalRequirements = additionalRequirements;
    }

    public void setJREUIs(Collection<IInstallableUnit> jreIUs) {
        this.jreIUs = jreIUs;
    }

    public abstract Collection<IInstallableUnit> resolve(IProgressMonitor monitor);
}
