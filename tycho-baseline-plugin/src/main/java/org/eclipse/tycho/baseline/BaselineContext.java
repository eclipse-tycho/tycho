/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.baseline;

import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.tycho.ArtifactKey;

public interface BaselineContext {

	void reportBaselineProblem(String message) throws MojoFailureException;

	List<String> getIgnores();

	List<String> getPackages();

	Logger getLogger();

	boolean isExtensionsEnabled();

	IArtifactRepository getArtifactRepository();

	IQueryable<IInstallableUnit> getMetadataRepository();

	ArtifactKey getArtifactKey();

}
