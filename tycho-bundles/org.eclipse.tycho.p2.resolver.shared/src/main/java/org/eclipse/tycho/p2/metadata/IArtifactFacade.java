/*******************************************************************************
 * Copyright (c) 2010, 2020 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *     Christoph Läubrich - enhance Javadoc
 *******************************************************************************/
package org.eclipse.tycho.p2.metadata;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Facade which provides an interface for common properties of a maven {@see Artifact} or
 * {@see MavenProject}. Needed to generate p2 metadata {@see P2Generator} for both reactor projects
 * and binary artifacts. For eclipse-plugin reactor projects, also carries information about the
 * corresponding eclipse source bundle. Implementors should:
 * <ul>
 * <li>provide {@link #hashCode()} and {@link #equals(Object)}</li>
 * <li>provide a description in {@link #toString()}</li>
 * <li>either be abstract or final unmodifiable classes</li>
 * </ul>
 */
public interface IArtifactFacade {
    public File getLocation();

    public String getGroupId();

    public String getArtifactId();

    public String getClassifier();

    public String getVersion();

    public String getPackagingType();

    default List<String> getDependencyTrail() {
        return Collections.emptyList();
    }

    /**
     * 
     * @return the id of the (remote) repository this artifact is located or <code>null</code> if
     *         unknown.
     */
    default String getRepository() {
        return null;
    }
}
