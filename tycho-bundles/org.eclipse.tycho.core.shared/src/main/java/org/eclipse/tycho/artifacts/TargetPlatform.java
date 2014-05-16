/*******************************************************************************
 * Copyright (c) 2011, 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.artifacts;

import java.io.File;

import org.eclipse.tycho.ArtifactKey;

/**
 * Set of artifacts which can be used by the build of a project, e.g. to resolve the project's
 * dependencies.
 * 
 * TODO 364134 What does it contain from the local reactor?
 */
// TODO only make final TP implement this interface?
public interface TargetPlatform {

    // TODO javadoc, in particular for version
    ArtifactKey resolveReference(String type, String id, String version);

    File getArtifactLocation(ArtifactKey artifact);

}
