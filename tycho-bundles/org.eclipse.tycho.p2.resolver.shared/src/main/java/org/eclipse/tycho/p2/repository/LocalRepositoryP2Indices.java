/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.p2.repository;

import java.io.File;

/**
 * This service provides access to the tycho p2 index files of the local maven repository.
 */
public interface LocalRepositoryP2Indices {

    public TychoRepositoryIndex getArtifactsIndex();

    public TychoRepositoryIndex getMetadataIndex();

    public File getBasedir();

}
