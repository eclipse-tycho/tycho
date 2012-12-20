/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - provide local artifact location; added documentation
 *******************************************************************************/
package org.eclipse.tycho.repository.gav;

import java.io.File;

import org.eclipse.tycho.p2.repository.GAV;

/**
 * Interface to obtain artifacts from GAV-indexed repositories.
 */
public interface GAVArtifactLocator {

    /**
     * Returns the permanent, local file system location of the identified artifact. This may
     * trigger a download of the artifact if necessary.
     * 
     * @return the local location of the artifact; never <code>null</code>
     */
    File getLocalArtifactLocation(GAV gav, String classifier, String extension);

}
