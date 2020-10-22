/*******************************************************************************
 * Copyright (c) 2008, 2014 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.util.resolution;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.core.shared.TargetEnvironment;

public abstract class AbstractResolutionStrategy {
    protected static final IInstallableUnit[] EMPTY_IU_ARRAY = new IInstallableUnit[0];

    protected final MavenLogger logger;

    protected ResolutionData data;

    protected AbstractResolutionStrategy(MavenLogger logger) {
        this.logger = logger;
    }

    public final void setData(ResolutionData data) {
        this.data = data;
    }

    public final Collection<IInstallableUnit> resolve(TargetEnvironment environment, IProgressMonitor monitor)
            throws ResolverException {
        return resolve(getEffectiveFilterProperties(environment), monitor);
    }

    public Collection<IInstallableUnit> multiPlatformResolve(List<TargetEnvironment> environments,
            IProgressMonitor monitor) throws ResolverException {
        Set<IInstallableUnit> result = new LinkedHashSet<>();

        for (TargetEnvironment environment : environments) {
            result.addAll(resolve(getEffectiveFilterProperties(environment), monitor));
        }

        return result;
    }

    protected abstract Collection<IInstallableUnit> resolve(Map<String, String> properties, IProgressMonitor monitor)
            throws ResolverException;

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

}
