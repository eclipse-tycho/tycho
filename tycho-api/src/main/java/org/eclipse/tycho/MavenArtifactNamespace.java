/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho;

public class MavenArtifactNamespace {

    /**
     * Namespace name for maven artifact capabilities and requirements.
     */
    public static final String MAVEN_ARTIFACT_NAMESPACE = "apache.maven.artifact";

    /**
     * The capability attribute identifying the group id.
     */
    public static final String CAPABILITY_GROUP_ATTRIBUTE = "group";

    /**
     * The capability attribute identifying the {@code Version} of the artifact.
     */
    public static final String CAPABILITY_VERSION_ATTRIBUTE = "version";
}
