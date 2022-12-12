/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.p2.metadata;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.tycho.IArtifactFacade;

public interface P2Generator {
    /**
     * @param artifacts
     * @param options
     * @param targetDir
     *            location to store artifacts created during meta data generation (e.g. root file
     *            zip)
     * @throws IOException
     */
    public Map<String, IP2Artifact> generateMetadata(List<IArtifactFacade> artifacts, PublisherOptions options,
            File targetDir) throws IOException;

    void persistMetadata(Map<String, IP2Artifact> metadata, File unitsXml, File artifactsXml) throws IOException;
}
