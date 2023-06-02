/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - provide local artifact location; added documentation
 *******************************************************************************/
package org.eclipse.tycho.p2.repository;

import java.io.File;

import org.eclipse.tycho.core.shared.MavenContext;

/**
 * Interface to obtain artifacts from GAV-indexed repositories.
 */
public interface RepositoryReader {

    /**
     * Returns the permanent, local file system location of the identified artifact. This may
     * trigger a download of the artifact if necessary.
     * 
     * @return the local location of the artifact; never <code>null</code>
     */
    File getLocalArtifactLocation(GAV gav, String classifier, String type);

    MavenContext getMavenContext();

}
