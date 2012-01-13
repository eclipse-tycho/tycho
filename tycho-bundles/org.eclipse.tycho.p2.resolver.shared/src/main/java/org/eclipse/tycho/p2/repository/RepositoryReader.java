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
package org.eclipse.tycho.p2.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Interface to obtain artifacts from GAV-indexed repositories.
 */
public interface RepositoryReader {

    /**
     * Returns the content of the identified artifact.
     * 
     * @return an input stream for reading the artifact; never <code>null</code>
     * @throws IOException
     *             if the artifact does not exist or cannot be obtained.
     */
    InputStream getContents(GAV gav, String classifier, String extension) throws IOException;

    /**
     * Returns the permanent, local file system location of the identified artifact. This may
     * trigger a download of the artifact if necessary.
     * 
     * @return the local location of the artifact; never <code>null</code>
     */
    File getLocalArtifactLocation(GAV gav, String classifier, String extension);

}
