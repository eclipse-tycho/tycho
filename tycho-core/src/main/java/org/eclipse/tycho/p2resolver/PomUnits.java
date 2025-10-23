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
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.query.CollectionResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.osgitools.targetplatform.ArtifactCollection;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.p2maven.InstallableUnitGenerator;
import org.eclipse.tycho.p2maven.InstallableUnitPublisher;
import org.eclipse.tycho.p2maven.advices.MavenChecksumAdvice;
import org.eclipse.tycho.p2maven.advices.MavenPropertiesAdvice;

@Named
@Singleton
public class PomUnits {

    private static final String KEY = PomUnits.class.getName() + "/dependencies";

    @Inject
    TychoProjectManager tychoProjectManager;

    @Inject
    InstallableUnitGenerator generator;

    @Inject
    InstallableUnitPublisher publisher;

    @Inject
    ArtifactHandlerManager artifactHandlerManager;

    @Inject
    Logger logger;

    public IQueryable<IInstallableUnit> createPomQueryable(ReactorProject reactorProject) {
        Optional<TychoProject> tychoProject = tychoProjectManager.getTychoProject(reactorProject);
        if (tychoProject.isEmpty()) {
            return new CollectionResult<>(Collections.emptyList());
        }
        TargetPlatformConfiguration configuration = tychoProjectManager
                .getTargetPlatformConfiguration(reactorProject.adapt(MavenProject.class));
        return reactorProject.computeContextValue(KEY, () -> {
            return new PomInstallableUnitStore(tychoProject.get(), reactorProject, generator, artifactHandlerManager,
                    logger, configuration);
        });
    }

    public void addCollectedUnits(PomDependencyCollector collector, ReactorProject reactorProject) {
        Optional<TychoProject> tychoProject = tychoProjectManager.getTychoProject(reactorProject);
        if (tychoProject.isEmpty()) {
            return;
        }
        Object contextValue = reactorProject.getContextValue(KEY);
        if (contextValue instanceof PomInstallableUnitStore store) {
            DependencyArtifacts dependencyArtifacts = tychoProject.get().getDependencyArtifacts(reactorProject);
            store.addPomDependencyConsumer(dependency -> {
                IArtifactFacade facade = dependency.artifactFacade();
                Entry<ArtifactKey, IArtifactDescriptor> result = collector.addMavenArtifact(facade,
                        dependency.installableUnit());
                if (result != null) {
                    List<IPublisherAdvice> advices = List.of(
                            new MavenPropertiesAdvice(facade.getGroupId(), facade.getArtifactId(), facade.getVersion(),
                                    facade.getClassifier(),
                                    artifactHandlerManager.getArtifactHandler(facade.getPackagingType()).getExtension(),
                                    facade.getPackagingType(), facade.getRepository()),
                            new MavenChecksumAdvice(facade.getLocation()));
                    publisher.applyAdvices(dependency.installableUnit(), result.getValue(), advices);
                    if (dependencyArtifacts instanceof ArtifactCollection collection) {
                        collection.addArtifactFile(result.getKey(), dependency.artifactFacade().getClassifier(),
                                dependency.location(), dependency.installableUnit());
                    }
                }
            });
        }
    }

}
