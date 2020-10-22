/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.metadata;

import java.io.File;

/**
 * Facade which provides an interface for common properties of a maven {@see Artifact} or {@see
 * MavenProject}. Needed to generate p2 metadata {@see P2Generator} for both reactor projects and
 * binary artifacts. For eclipse-plugin reactor projects, also carries information about the
 * corresponding eclipse source bundle.
 */
public interface IArtifactFacade {
    public File getLocation();

    public String getGroupId();

    public String getArtifactId();

    public String getClassifier();

    public String getVersion();

    public String getPackagingType();
}
