/*******************************************************************************
 * Copyright (c) 2008, 2013 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver.facade;

import java.util.List;
import java.util.Map;

import org.eclipse.tycho.ReactorProject;
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

    public void addDependency(String type, String id, String versionRange);

    /**
     * Returns list ordered of resolution result, one per requested TargetEnvironment.
     * 
     * @param project
     *            The reactor project to be resolved. May be <code>null</code>, in which case only
     *            the additional dependencies are resolved.
     * 
     * @see #addDependency(String, String, String)
     * @TODO this should return Map<TargetEnvironment,P2ResolutionResult>
     */
    public List<P2ResolutionResult> resolveDependencies(TargetPlatform context, ReactorProject project);

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
