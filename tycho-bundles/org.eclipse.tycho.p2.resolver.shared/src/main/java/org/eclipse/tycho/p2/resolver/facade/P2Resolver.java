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
package org.eclipse.tycho.p2.resolver.facade;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.p2.target.facade.TargetPlatformBuilder;

public interface P2Resolver {
    /**
     * Pseudo artifact type used to denote P2 installable unit dependencies
     * 
     * @see ArtifactKey
     */
    public static final String TYPE_INSTALLABLE_UNIT = "p2-installable-unit";

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
     * @param reactorProjectLocation
     *            may be <code>null</code>
     * @TODO this should return Map<TargetEnvironment,P2ResolutionResult>
     */
    public List<P2ResolutionResult> resolveDependencies(TargetPlatform context, File reactorProjectLocation);

    @Deprecated
    public P2ResolutionResult collectProjectDependencies(TargetPlatform context, File projectLocation);

    public P2ResolutionResult resolveMetadata(TargetPlatformBuilder context);

    /**
     * Resolves specified installable unit identified by id and versionRange. The unit with latest
     * version is return if id/versionRange match multiple units.
     */
    public P2ResolutionResult resolveInstallableUnit(TargetPlatform context, String id, String versionRange);

}
