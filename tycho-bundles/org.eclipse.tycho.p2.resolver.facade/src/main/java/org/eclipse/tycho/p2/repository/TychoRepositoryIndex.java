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
package org.eclipse.tycho.p2.repository;

import java.io.IOException;
import java.util.Set;

public interface TychoRepositoryIndex {

    /**
     * Receive the set of GAVs contained in this index
     * 
     * @return an unmodifiable defensive copy of the GAV set contained in this index
     */
    Set<GAV> getProjectGAVs();

    /**
     * Adds a GAV to the index
     * 
     * @see {@link #save()}
     * @param gav
     *            not <code>null</code>
     */
    void addGav(GAV gav);

    /**
     * Remove a GAV from the index.
     * 
     * @param gav
     * @see {@link #save()}
     */
    void removeGav(GAV gav);

    /**
     * Changes performed via {@link #addGav(GAV)} , {@link #removeGav(GAV)} will only be reflected
     * in the memory state of the index. In case the index is bound some persistence location (e.g.
     * a file see {@link FileBasedTychoRepositoryIndex#createArtifactsIndex(java.io.File)}) the
     * method will store the current memory content to the persistence storage.
     * 
     * @throws IOException
     */
    void save() throws IOException;

}
