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
package org.eclipse.tycho;

/**
 * Eclipse/OSGi artifact key. Contains the "coordinates" which identify an artifact in the Eclipse
 * universe.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ArtifactKey {

    /**
     * Artifact type. Should be one of the {@link ArtifactType} constants.
     */
    // TODO really restrict to ArtifactType values; currently may also be a value from PackagingType
    public String getType();

    /**
     * Eclipse/OSGi artifact id (bundle symbolic name, feature id, etc). Can differ from Maven
     * artifactId.
     */
    public String getId();

    /**
     * Eclipse/OSGi artifact version. Can differ from Maven version. For maven projects, this
     * version corresponds to version specified in the project sources and does not reflect
     * qualifier expansion.
     */
    public String getVersion();

}
