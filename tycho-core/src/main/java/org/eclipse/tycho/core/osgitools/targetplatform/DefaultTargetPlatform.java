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
package org.eclipse.tycho.core.osgitools.targetplatform;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.DependencyArtifacts;

// TODO 364134 rename this class
public class DefaultTargetPlatform extends BasicDependencyArtifacts implements DependencyArtifacts {

    /**
     * ArtifactKey cache used to correlate equal instances to reduce memory usage
     */
    private static final WeakHashMap<ArtifactKey, ArtifactKey> KEY_CACHE = new WeakHashMap<ArtifactKey, ArtifactKey>();

    /**
     * ArtifactDescriptor cache used to correlate equal instances to reduce memory usage
     */
    private static final WeakHashMap<ArtifactDescriptor, ArtifactDescriptor> ARTIFACT_CACHE = new WeakHashMap<ArtifactDescriptor, ArtifactDescriptor>();

    /**
     * 'this' project, i.e. the project the dependencies were resolved for. can be null.
     */
    protected final ReactorProject project;

    /**
     * Set of installable unit in the target platform of the module that do not come from the local
     * reactor.
     */
    protected final Set<Object/* IInstallableUnit */> nonReactorUnits = new LinkedHashSet<Object>();

    public DefaultTargetPlatform() {
        this(null);
    }

    public DefaultTargetPlatform(ReactorProject project) {
        this.project = project;
    }

    @Override
    protected ArtifactDescriptor normalize(ArtifactDescriptor artifact) {
        ArtifactDescriptor cachedArtifact = ARTIFACT_CACHE.get(artifact);
        if (cachedArtifact != null) {
            artifact = cachedArtifact;
        } else {
            ARTIFACT_CACHE.put(artifact, artifact);
        }
        return artifact;
    }

    @Override
    protected ArtifactKey normalize(ArtifactKey key) {
        ArtifactKey cachedKey = KEY_CACHE.get(key);
        if (cachedKey != null) {
            key = cachedKey;
        } else {
            KEY_CACHE.put(key, key);
        }
        return key;
    }

    public Set<?/* IInstallableUnit */> getNonReactorUnits() {
        return nonReactorUnits;
    }

    public Set<?/* IInstallableUnit */> getInstallableUnits() {
        Set<Object> units = new LinkedHashSet<Object>();
        for (ArtifactDescriptor artifact : artifacts.values()) {
            if (project == null || !project.equals(artifact.getMavenProject())) {
                units.addAll(artifact.getInstallableUnits());
            }
        }
        units.addAll(nonReactorUnits);
        return Collections.unmodifiableSet(units);
    }

    public void addNonReactorUnits(Set<?/* IInstallableUnit */> installableUnits) {
        this.nonReactorUnits.addAll(installableUnits);
    }

}
