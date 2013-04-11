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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.core.facade.TargetEnvironment;
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

    public final void setEEResolutionHints(ExecutionEnvironmentResolutionHints eeResolutionHints) {
        modifiableData.setEEResolutionHints(eeResolutionHints);
    }

    public final void setAdditionalFilterProperties(Map<String, String> additionalFilterProperties) {
        modifiableData.setAdditionalFilterProperties(additionalFilterProperties);
    }

    public Collection<IInstallableUnit> resolve(TargetEnvironment environment, IProgressMonitor monitor) {
        return resolve(getEffectiveFilterProperties(environment), monitor);
    }

    public Collection<IInstallableUnit> multiPlatformResolve(List<TargetEnvironment> environments,
            IProgressMonitor monitor) {
        Set<IInstallableUnit> result = new LinkedHashSet<IInstallableUnit>();

        for (TargetEnvironment environment : environments) {
            result.addAll(resolve(getEffectiveFilterProperties(environment), monitor));
        }

        return result;
    }

    protected abstract Collection<IInstallableUnit> resolve(Map<String, String> properties, IProgressMonitor monitor);

    private Map<String, String> getEffectiveFilterProperties(TargetEnvironment environment) {
        Map<String, String> result = environment.toFilterProperties();
        result.put("org.eclipse.update.install.features", "true");
        insertAdditionalFilterProperties(result);
        return result;
    }

    private void insertAdditionalFilterProperties(Map<String, String> result) {
        for (Entry<String, String> entry : data.getAdditionalFilterProperties().entrySet()) {
            String overwrittenValue = result.put(entry.getKey(), entry.getValue());

            if (overwrittenValue != null) {
                logger.warn("Overriding profile property '" + entry.getKey() + "' with value '" + entry.getValue()
                        + "' (was '" + overwrittenValue + "')");
            }
        }
    }

    protected RuntimeException newResolutionException(IStatus status) {
        return new RuntimeException(StatusTool.collectProblems(status), status.getException());
    }
}
