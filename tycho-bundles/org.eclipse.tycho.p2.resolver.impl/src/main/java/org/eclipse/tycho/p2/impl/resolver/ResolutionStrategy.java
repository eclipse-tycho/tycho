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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.util.StatusTool;

@SuppressWarnings("restriction")
public abstract class ResolutionStrategy {
    protected IQueryable<IInstallableUnit> availableIUs;

    protected Collection<IInstallableUnit> jreIUs;

    protected Collection<IInstallableUnit> rootIUs;

    protected List<IRequirement> additionalRequirements;

    protected final MavenLogger logger;

    protected ResolutionStrategy(MavenLogger logger) {
        this.logger = logger;
    }

    public void setAvailableInstallableUnits(IQueryable<IInstallableUnit> availableIUs) {
        this.availableIUs = availableIUs;
    }

    public void setRootInstallableUnits(Collection<IInstallableUnit> rootIUs) {
        this.rootIUs = rootIUs;
    }

    public void setAdditionalRequirements(List<IRequirement> additionalRequirements) {
        this.additionalRequirements = additionalRequirements;
    }

    public void setJREUIs(Collection<IInstallableUnit> jreIUs) {
        this.jreIUs = jreIUs;
    }

    public Collection<IInstallableUnit> resolve(List<Map<String, String>> allproperties, IProgressMonitor monitor) {
        Set<IInstallableUnit> result = new LinkedHashSet<IInstallableUnit>();

        for (Map<String, String> properties : allproperties) {
            result.addAll(resolve(properties, monitor));
        }

        return result;
    }

    public abstract Collection<IInstallableUnit> resolve(Map<String, String> properties, IProgressMonitor monitor);

    protected Set<IInstallableUnit> createAdditionalRequirementsIU() {
        LinkedHashSet<IInstallableUnit> result = new LinkedHashSet<IInstallableUnit>();

        if (additionalRequirements != null && !additionalRequirements.isEmpty()) {
            InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
            String time = Long.toString(System.currentTimeMillis());
            iud.setId("tycho-extra-" + time);
            iud.setVersion(Version.createOSGi(0, 0, 0, time));
            iud.setRequirements(additionalRequirements.toArray(new IRequiredCapability[additionalRequirements.size()]));

            result.add(MetadataFactory.createInstallableUnit(iud));
        }

        return result;
    }

    protected Map<String, String> addFeatureJarFilter(Map<String, String> environment) {
        final Map<String, String> selectionContext = new HashMap<String, String>(environment);
        selectionContext.put("org.eclipse.update.install.features", "true");
        return selectionContext;
    }

    protected RuntimeException newResolutionException(IStatus status) {
        return new RuntimeException(StatusTool.collectProblems(status), status.getException());
    }

}
