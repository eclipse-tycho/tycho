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
package org.eclipse.tycho.p2.resolver.facade;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.IllegalArtifactReferenceException;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;

public interface P2Resolver {
    public static final String ANY_QUALIFIER = "qualifier";

    public void setEnvironments(List<TargetEnvironment> environments);

    /**
     * Sets additional properties that will be used to evaluate filter expressions in the p2
     * metadata. These properties are also known as "profile properties" in p2.
     */
    public void setAdditionalFilterProperties(Map<String, String> filterProperties);

    public void addDependency(String type, String id, String versionRange) throws IllegalArtifactReferenceException;

    /**
     * Returns list ordered of resolution result, one per requested TargetEnvironment.
     * 
     * @param project
     *            The reactor project to be resolved. May be <code>null</code>, in which case only
     *            the additional dependencies are resolved.
     * 
     * @see #addDependency(String, String, String)
     */
    public Map<TargetEnvironment, P2ResolutionResult> resolveTargetDependencies(TargetPlatform context,
            ReactorProject project);

    /**
     * @deprecated use {@link #resolveTargetDependencies(TargetPlatform, ReactorProject)} instead
     */
    @Deprecated
    default List<P2ResolutionResult> resolveDependencies(TargetPlatform context, ReactorProject project) {
        return new ArrayList<>(resolveTargetDependencies(context, project).values());
    }

    /**
     * @deprecated Only needed for the deprecated eclipse-update-site (see bug 342876)
     */
    // TODO 403481 replace the "conflicting dependency aggregation" feature of eclipse-update-site 
    @Deprecated
    public P2ResolutionResult collectProjectDependencies(TargetPlatform context, ReactorProject project);

    public P2ResolutionResult resolveMetadata(TargetPlatformConfigurationStub context, String executionEnvironmentName);

    public P2ResolutionResult getTargetPlatformAsResolutionResult(TargetPlatformConfigurationStub tpConfiguration,
            String eeName);

    /**
     * Resolves specified installable unit identified by id and versionRange. The unit with latest
     * version is return if id/versionRange match multiple units.
     */
    public P2ResolutionResult resolveInstallableUnit(TargetPlatform context, String id, String versionRange);

}
