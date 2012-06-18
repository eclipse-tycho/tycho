/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - split target platform computation and dependency resolution
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.resolver;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.artifacts.TargetPlatformFilter;
import org.eclipse.tycho.artifacts.p2.P2TargetPlatform;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.metadata.IReactorArtifactFacade;
import org.eclipse.tycho.p2.target.filters.TargetPlatformFilterEvaluator;

class ResolutionContext {

    private final Map<ClassifiedLocation, IReactorArtifactFacade> reactorProjects = new LinkedHashMap<ClassifiedLocation, IReactorArtifactFacade>();

    private final List<TargetPlatformFilter> iuFilters = new ArrayList<TargetPlatformFilter>();
    private TargetPlatformFilterEvaluator filterEvaluator;

    private final MavenLogger logger;

    private P2TargetPlatform externalTP;

    ResolutionContext(MavenLogger logger) {
        this.logger = logger;
    }

    public void addFilters(List<TargetPlatformFilter> filters) {
        this.iuFilters.addAll(filters);

        this.filterEvaluator = !iuFilters.isEmpty() ? new TargetPlatformFilterEvaluator(iuFilters, logger) : null;
    }

    public void addReactorArtifact(IReactorArtifactFacade artifact) {
        ClassifiedLocation key = new ClassifiedLocation(artifact);

//        if (reactorProjects.containsKey(key)) {
//            throw new IllegalStateException();
//        }

        reactorProjects.put(key, artifact);
    }

    public void setExternalTargetPlatform(P2TargetPlatform externalTP) {
        this.externalTP = externalTP;
    }

    public Collection<IInstallableUnit> getInstallableUnits() {
        Collection<IInstallableUnit> result = new ArrayList<IInstallableUnit>();
        Set<IInstallableUnit> reactorUnits = getReactorProjectIUs().keySet();
        result.addAll(reactorUnits);

        if (externalTP != null) {
            Collection<IInstallableUnit> externalUnits = externalTP.getInstallableUnits();
            applyFilters(filterEvaluator, externalUnits, reactorUnits);
            result.addAll(externalUnits);
        }
        return result;
    }

    // also checks for duplicate IUs
    // TODO should this result be cached?
    private Map<IInstallableUnit, IReactorArtifactFacade> getReactorProjectIUs() {
        Map<IInstallableUnit, Set<File>> unitToProjectMap = new HashMap<IInstallableUnit, Set<File>>();
        Map<IInstallableUnit, Set<File>> duplicateReactorUnits = new HashMap<IInstallableUnit, Set<File>>();
        Map<IInstallableUnit, IReactorArtifactFacade> allUnits = new LinkedHashMap<IInstallableUnit, IReactorArtifactFacade>();

        for (IReactorArtifactFacade project : reactorProjects.values()) {
            LinkedHashSet<IInstallableUnit> projectUnits = new LinkedHashSet<IInstallableUnit>();

            for (Object iu : project.getDependencyMetadata(true)) {
                projectUnits.add((IInstallableUnit) iu);
                allUnits.put((IInstallableUnit) iu, project);
            }
            for (Object iu : project.getDependencyMetadata(false)) {
                projectUnits.add((IInstallableUnit) iu);
                allUnits.put((IInstallableUnit) iu, project);
            }

            for (IInstallableUnit unit : projectUnits) {
                Set<File> projects = unitToProjectMap.get(unit);
                if (projects == null) {
                    projects = new LinkedHashSet<File>();
                    unitToProjectMap.put(unit, projects);
                }
                projects.add(project.getLocation());
                if (projects.size() > 1) {
                    duplicateReactorUnits.put(unit, projects);
                }
            }
        }

        if (!duplicateReactorUnits.isEmpty()) {
            throw new DuplicateReactorIUsException(duplicateReactorUnits);
        }

        filterUnits(allUnits.keySet());

        return Collections.unmodifiableMap(allUnits);
    }

    public Collection<IInstallableUnit> getReactorProjectIUs(File projectRoot, boolean primary) {
        boolean found = false;
        LinkedHashSet<IInstallableUnit> result = new LinkedHashSet<IInstallableUnit>();
        for (IReactorArtifactFacade project : reactorProjects.values()) {
            if (project.getLocation().equals(projectRoot)) {
                found = true;
                result.addAll(toSet(project.getDependencyMetadata(primary), IInstallableUnit.class));
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Not a reactor project: " + projectRoot);
        }
        filterUnits(result);
        return Collections.unmodifiableSet(result);
    }

    public IArtifactFacade getMavenArtifact(IInstallableUnit iu) {
        IArtifactFacade artifact = getReactorProjectIUs().get(iu);

        if (artifact == null && externalTP != null) {
            artifact = externalTP.getMavenArtifact(iu);
        }
        return artifact;
    }

    private void applyFilters(TargetPlatformFilterEvaluator filter, Collection<IInstallableUnit> availableUnits,
            Set<IInstallableUnit> reactorProjectUIs) {

        Set<String> reactorIUIDs = new HashSet<String>();
        for (IInstallableUnit unit : reactorProjectUIs) {
            reactorIUIDs.add(unit.getId());
        }

        // installable units shadowed by reactor projects
        Iterator<IInstallableUnit> iter = availableUnits.iterator();
        while (iter.hasNext()) {
            IInstallableUnit unit = iter.next();
            if (reactorIUIDs.contains(unit.getId())) {
                // TODO log
                iter.remove();
                continue;
            }
        }

        // configured filters
        if (filter != null) {
            filter.filterUnits(availableUnits);
        }
    }

    private void filterUnits(Collection<IInstallableUnit> keySet) {
        if (filterEvaluator != null) {
            filterEvaluator.filterUnits(keySet);
        }
    }

    static <T> Set<T> toSet(Collection<Object> collection, Class<T> targetType) {
        if (collection == null || collection.isEmpty()) {
            return Collections.emptySet();
        }

        LinkedHashSet<T> set = new LinkedHashSet<T>();

        for (Object o : collection) {
            set.add(targetType.cast(o));
        }

        return set;
    }

}
