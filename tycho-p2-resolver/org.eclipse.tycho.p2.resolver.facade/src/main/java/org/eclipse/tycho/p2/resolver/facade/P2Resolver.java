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

public interface P2Resolver {
    /** @see org.eclipse.tycho.ArtifactKey */
    public static final String TYPE_ECLIPSE_PLUGIN = "eclipse-plugin";

    /** @see org.eclipse.tycho.ArtifactKey */
    public static final String TYPE_ECLIPSE_FEATURE = "eclipse-feature";

    /** @see org.eclipse.tycho.ArtifactKey */
    public static final String TYPE_ECLIPSE_TEST_PLUGIN = "eclipse-test-plugin";

    /** @see org.eclipse.tycho.ArtifactKey */
    public static final String TYPE_ECLIPSE_APPLICATION = "eclipse-application";

    /** @see org.eclipse.tycho.ArtifactKey */
    public static final String TYPE_ECLIPSE_UPDATE_SITE = "eclipse-update-site";

    /** @see org.eclipse.tycho.ArtifactKey */
    public static final String TYPE_ECLIPSE_REPOSITORY = "eclipse-repository";

    /**
     * Pseudo artifact type used to denote P2 installable unit dependencies
     */
    public static final String TYPE_INSTALLABLE_UNIT = "p2-installable-unit";

    public static final String ANY_QUALIFIER = "qualifier";

    public void setEnvironments(List<Map<String, String>> properties);

    public void addDependency(String type, String id, String versionRange);

    public List<P2ResolutionResult> resolveProject(ResolutionContext context, File location);

    public P2ResolutionResult collectProjectDependencies(ResolutionContext context, File projectLocation);

    public P2ResolutionResult resolveMetadata(ResolutionContext context, Map<String, String> properties);
}
