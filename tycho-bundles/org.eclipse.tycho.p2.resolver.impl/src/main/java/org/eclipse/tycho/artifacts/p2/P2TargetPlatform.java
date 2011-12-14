/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.artifacts.p2;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;

/**
 * Specialization of the {@link TargetPlatform} interface for a target platform which includes p2
 * metadata.
 */
public interface P2TargetPlatform extends TargetPlatform {

    IQueryable<IInstallableUnit> getInstallableUnits();

    /**
     * Return IUs that represent packages provided by target JRE
     */
    Collection<IInstallableUnit> getJREIUs();

    /**
     * Notify the target platform implementation about which IUs are actually used. This for example
     * allows debug output and the preparation of caches.
     */
    void reportUsedIUs(Collection<IInstallableUnit> usedUnits);

    File getLocalArtifactFile(IArtifactKey key);

    // TODO 364134 revise the relationship of target platform and dependency only IUs
    LinkedHashSet<IInstallableUnit> getReactorProjectIUs(File projectLocation, boolean primary);

    IArtifactFacade getMavenArtifact(IInstallableUnit iu);

}
