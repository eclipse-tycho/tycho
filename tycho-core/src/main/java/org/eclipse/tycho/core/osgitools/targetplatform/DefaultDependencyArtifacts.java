/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - Issue #626 - Classpath computation must take fragments into account 
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools.targetplatform;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.osgitools.DefaultArtifactDescriptor;

public class DefaultDependencyArtifacts extends ArtifactCollection implements DependencyArtifacts {

    /**
     * ArtifactKey cache used to correlate equal instances to reduce memory usage
     */
    private static final Map<ArtifactKey, ArtifactKey> KEY_CACHE = new ConcurrentHashMap<>();

    /**
     * ArtifactDescriptor cache used to correlate equal instances to reduce memory usage
     */
    private static final Map<ArtifactDescriptor, ArtifactDescriptor> ARTIFACT_CACHE = new ConcurrentHashMap<>();

    protected final List<ArtifactDescriptor> fragments = new ArrayList<>();

    /**
     * 'this' project, i.e. the project the dependencies were resolved for. can be null.
     */
    protected final ReactorProject project;

    /**
     * Set of installable unit in the target platform of the module that do not come from the local
     * reactor.
     */
    protected final Set<Object/* IInstallableUnit */> nonReactorUnits = new LinkedHashSet<>();

    public DefaultDependencyArtifacts() {
        this(null);
    }

    public DefaultDependencyArtifacts(ReactorProject project) {
        this.project = project;
    }

    @Override
    protected ArtifactDescriptor normalize(ArtifactDescriptor artifact) {
        ArtifactDescriptor cachedArtifact = ARTIFACT_CACHE.putIfAbsent(artifact, artifact);
        return (cachedArtifact != null) ? cachedArtifact : artifact;
    }

    @Override
    protected ArtifactKey normalize(ArtifactKey key) {
        ArtifactKey cachedKey = KEY_CACHE.putIfAbsent(key, key);
        return (cachedKey != null) ? cachedKey : key;
    }

    @Override
    public Set<?/* IInstallableUnit */> getNonReactorUnits() {
        return nonReactorUnits;
    }

    @Override
    public Set<?/* IInstallableUnit */> getInstallableUnits() {
        Set<Object> units = new LinkedHashSet<>();
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

    public void addFragment(ArtifactKey key, Supplier<File> location, Set<Object> installableUnits) {
        fragments.add(new DefaultArtifactDescriptor(key, whatever -> location.get(), null, null, installableUnits));
    }

    @Override
    public Collection<ArtifactDescriptor> getFragments() {
        return Collections.unmodifiableCollection(fragments);
    }

}
