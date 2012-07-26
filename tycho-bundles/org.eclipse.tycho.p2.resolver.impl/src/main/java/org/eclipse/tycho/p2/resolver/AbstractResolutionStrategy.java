/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.util.StatusTool;

// TODO make this package private
public abstract class AbstractResolutionStrategy {
    protected static final IInstallableUnit[] EMPTY_IU_ARRAY = new IInstallableUnit[0];

    protected final MavenLogger logger;

    // TODO take ResolutionData as parameter to get rid of this construct
    private final ResolutionDataImpl modifiableData = new ResolutionDataImpl();
    protected final ResolutionData data = modifiableData;

    protected AbstractResolutionStrategy(MavenLogger logger) {
        this.logger = logger;
    }

    public final void setAvailableInstallableUnits(Collection<IInstallableUnit> availableIUs) {
        modifiableData.setAvailableIUs(availableIUs);
    }

    public final void setRootInstallableUnits(Collection<IInstallableUnit> rootIUs) {
        modifiableData.setRootIUs(rootIUs);
    }

    public final void setAdditionalRequirements(List<IRequirement> additionalRequirements) {
        modifiableData.setAdditionalRequirements(additionalRequirements);
    }

    public final void setJREIUs(Collection<IInstallableUnit> jreIUs) {
        modifiableData.setJreIUs(jreIUs);
    }

    public Collection<IInstallableUnit> multiPlatformResolve(List<Map<String, String>> allproperties, IProgressMonitor monitor) {
        Set<IInstallableUnit> result = new LinkedHashSet<IInstallableUnit>();

        for (Map<String, String> properties : allproperties) {
            result.addAll(resolve(properties, monitor));
        }

        return result;
    }

    public abstract Collection<IInstallableUnit> resolve(Map<String, String> properties, IProgressMonitor monitor);

    protected static Map<String, String> addFeatureJarFilter(Map<String, String> environment) {
        final Map<String, String> selectionContext = new HashMap<String, String>(environment);
        selectionContext.put("org.eclipse.update.install.features", "true");
        return selectionContext;
    }

    protected RuntimeException newResolutionException(IStatus status) {
        return new RuntimeException(StatusTool.collectProblems(status), status.getException());
    }
}
