/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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

/**
 * describes a dependency as with the maven Dependency class
 */
public interface MavenDependencyDescriptor {

    /**
     * @return the unique id for an artifact
     */
    String getArtifactId();

    /**
     * @return the classifier of the dependency
     */
    String getClassifier();

    /**
     * 
     * @return group that produced the dependency
     */
    String getGroupId();

    /**
     * @return the type of dependency
     */
    String getType();

    /**
     * @return the version of the dependency
     */
    String getVersion();

    /**
     * 
     * @return the id of the repository this artifact is located in
     */
    String getRepository();
}
