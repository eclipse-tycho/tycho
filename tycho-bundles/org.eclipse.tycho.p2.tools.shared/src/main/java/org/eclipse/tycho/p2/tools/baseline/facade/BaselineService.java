/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
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
package org.eclipse.tycho.p2.tools.baseline.facade;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.p2.metadata.IP2Artifact;

public interface BaselineService {

    public Map<String, IP2Artifact> getProjectBaseline(Collection<MavenRepositoryLocation> baselineLocations,
            Map<String, IP2Artifact> reactor, File target);

    public boolean isMetadataEqual(IP2Artifact ip2Artifact, IP2Artifact ip2Artifact2);

}
