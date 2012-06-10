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
package org.eclipse.tycho.p2.metadata;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface P2Generator {
    /**
     * @param artifacts
     * @param targetDir
     *            location to store artifacts created during meta data generation (e.g. root file
     *            zip)
     * @throws IOException
     */
    public Map<String, IP2Artifact> generateMetadata(List<IArtifactFacade> artifacts,
            File targetDir) throws IOException;

    void persistMetadata(Map<String, IP2Artifact> metadata, File unitsXml, File artifactsXml) throws IOException;
}
