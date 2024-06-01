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

import static java.util.Optional.ofNullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.query.CollectionResult;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.maven.MavenArtifactFacade;
import org.eclipse.tycho.core.resolver.shared.PomDependencies;
import org.eclipse.tycho.p2.metadata.ReactorProjectFacade;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.resolver.WrappedArtifact;
import org.eclipse.tycho.p2maven.InstallableUnitGenerator;
import org.eclipse.tycho.p2tools.copiedfromp2.QueryableArray;

class PomInstallableUnitStore implements IQueryable<IInstallableUnit> {

    private static final IQueryResult<IInstallableUnit> EMPTY_RESULT = new CollectionResult<>(Collections.emptyList());
    private IQueryable<IInstallableUnit> collection;
    private TychoProject tychoProject;
    private ReactorProject reactorProject;
    private Map<IInstallableUnit, PomDependency> installableUnitLookUp = new HashMap<>();
    private Collection<PomDependency> gatheredDependencies = new HashSet<>();
    private List<Consumer<PomDependency>> dependencyConsumer = new ArrayList<>();
    private InstallableUnitGenerator generator;
    private PomDependencies considerPomDependencies;
    private ArtifactHandlerManager artifactHandlerManager;
    private Logger logger;
    private TargetPlatformConfiguration configuration;

    public PomInstallableUnitStore(TychoProject tychoProject, ReactorProject reactorProject,
            InstallableUnitGenerator generator, ArtifactHandlerManager artifactHandlerManager, Logger logger,
            TargetPlatformConfiguration configuration) {
        this.tychoProject = tychoProject;
        this.reactorProject = reactorProject;
        this.generator = generator;
        this.artifactHandlerManager = artifactHandlerManager;
        this.logger = logger;
        this.configuration = configuration;
        this.considerPomDependencies = ofNullable(configuration)//
                .map(TargetPlatformConfiguration::getPomDependencies)//
                .orElse(PomDependencies.ignore);
    }

    @Override
    public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
        if (considerPomDependencies == PomDependencies.ignore) {
            return EMPTY_RESULT;
        }
        IQueryResult<IInstallableUnit> result = getPomIUs().query(query, monitor);
        for (IInstallableUnit unit : result) {
            PomDependency pomDependency = installableUnitLookUp.get(unit);
            if (pomDependency == null) {
                continue;
            }
            if (gatheredDependencies.add(pomDependency)) {
                for (Consumer<PomDependency> consumer : dependencyConsumer) {
                    consumer.accept(pomDependency);
                }
            }
        }
        return result;
    }

    static final record PomDependency(IArtifactFacade artifactFacade, Collection<IInstallableUnit> installableUnit,
            File location) {
    }

    private IQueryable<IInstallableUnit> getPomIUs() {
        if (collection == null) {
            PublisherInfo publisherInfo = new PublisherInfo();
            publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX);
            Collection<Artifact> initalArtifacts = new ArrayList<>(
                    tychoProject.getInitialArtifacts(reactorProject, List.of(Artifact.SCOPE_TEST)));
            addBuildReactorProjects(initalArtifacts);
            Map<Artifact, IArtifactFacade> facadeMap = tychoProject.getArtifactFacades(reactorProject, initalArtifacts);
            for (Artifact artifact : initalArtifacts) {
                IArtifactFacade facade = facadeMap.get(artifact);
                getArtifactStream(artifact, facade).forEach(a -> {
                    Collection<IInstallableUnit> units;
                    if (a.getFile() == null) {
                        if (facadeMap.get(a) instanceof ReactorProjectFacade reactorFacade) {
                            ReactorProject prj = reactorFacade.getReactorProject();
                            units = generator.getProvidedInstallableUnits(prj);
                            if (units.isEmpty()) {
                                logger.debug("Skip artifact " + a
                                        + " because it is not resolved and can't gather any units for it...");
                                return;
                            }
                        } else {
                            logger.debug("Skip artifact " + a + " because it is not resolved!");
                            return;
                        }
                    } else if (configuration.isExcluded(a.getGroupId(), a.getArtifactId())) {
                        logger.debug("Skip artifact " + a + " because it is excluded...");
                        return;
                    } else {
                        units = generator.getInstallableUnits(a);
                    }
                    logger.debug("artifact " + a + " maps to " + units);
                    IArtifactFacade artifactFacade;
                    if (a.hasClassifier()) {
                        artifactFacade = new MavenArtifactFacade(a);
                    } else {
                        artifactFacade = facade;
                    }
                    boolean wrapHasErrors = false;
                    if (considerPomDependencies == PomDependencies.wrapAsBundle && units.isEmpty() && a == artifact) {
                        String relativePath = RepositoryLayoutHelper.getRelativePath(a.getGroupId(), a.getArtifactId(),
                                a.getVersion(), WrappedArtifact.createClassifierFromArtifact(a.getClassifier()),
                                artifactHandlerManager.getArtifactHandler(a.getType()).getExtension());
                        //TODO currently we need to store this in the basedir(!) because otherwise a clean will wipe out the data!
                        //it would be better to only generate the metadata here and write the final jar when the data is requested the first time!
                        File wrappedFile = new File(new File(reactorProject.getBasedir(), ".m2"), relativePath);
                        //check that it might be a bundle...
                        try {
                            WrappedArtifact wrappedArtifact = WrappedArtifact.createWrappedArtifact(artifactFacade,
                                    reactorProject.getGroupId(), wrappedFile);
                            artifactFacade = wrappedArtifact;
                            File wrappedLocation = artifactFacade.getLocation();
                            a.setFile(wrappedLocation);
                            units = generator.getInstallableUnits(a);
                            logger.warn("Maven Artifact " + a.getGroupId() + ":" + a.getArtifactId() + ":"
                                    + a.getVersion()
                                    + " is not a bundle and was automatically wrapped with bundle-symbolic name "
                                    + wrappedArtifact.getWrappedBsn()
                                    + ", ignoring such artifacts can be enabled with <pomDependencies>"
                                    + PomDependencies.consider
                                    + "</pomDependencies> in target platform configuration.");
                            logger.info(wrappedArtifact.getReferenceHint());
                        } catch (Exception e) {
                            wrapHasErrors = true;
                            //can't wrap then...
                            if (logger.isDebugEnabled()) {
                                logger.error("wrapping " + a.getId() + " @ " + a.getFile() + " failed and is ignored",
                                        e);
                            } else {
                                logger.warn("wrapping " + a.getId()
                                        + " failed and is ignored, enable debug output for details.");
                            }
                        }
                    }
                    if (units.isEmpty()) {
                        if (a == artifact && considerPomDependencies != PomDependencies.wrapAsBundle
                                && !wrapHasErrors) {
                            //only report this for the main artifact!
                            logger.info("Maven Artifact " + a.getGroupId() + ":" + a.getArtifactId() + ":"
                                    + a.getVersion() + " @ " + a.getFile()
                                    + " is not a bundle and will be ignored, automatic wrapping of such artifacts can be enabled with "
                                    + "<pomDependencies>" + PomDependencies.wrapAsBundle.name()
                                    + "</pomDependencies> in target platform configuration.");
                        }
                    } else {
                        PomDependency value;
                        if (a.hasClassifier()) {
                            value = new PomDependency(new MavenArtifactFacade(a), units, a.getFile());
                        } else {
                            value = new PomDependency(facade, units, a.getFile());
                        }
                        for (IInstallableUnit unit : units) {
                            installableUnitLookUp.put(unit, value);
                        }
                    }
                });
            }
            collection = new QueryableArray(installableUnitLookUp.keySet(), false);
        }
        return collection;
    }

    private void addBuildReactorProjects(Collection<Artifact> initalArtifacts) {
        MavenSession mavenSession = reactorProject.adapt(MavenSession.class);
        if (mavenSession != null) {
            for (MavenProject mavenProject : mavenSession.getProjects()) {
                if ("jar".equals(mavenProject.getPackaging())) {
                    initalArtifacts.add(mavenProject.getArtifact());
                }
            }
        }
    }

    private Stream<Artifact> getArtifactStream(Artifact artifact, IArtifactFacade facade) {
        if (facade instanceof ReactorProjectFacade projectFacade) {
            MavenProject mavenProject = projectFacade.getReactorProject().adapt(MavenProject.class);
            if (mavenProject != null) {
                return Stream.concat(Stream.of(mavenProject.getArtifact()),
                        safeCopy(mavenProject.getAttachedArtifacts()).stream());
            }
        }
        return Stream.of(artifact);

    }

    private List<Artifact> safeCopy(List<Artifact> list) {
        while (true) {
            //in parallel execution mode it is possible that items are added to the attached artifacts what will throw ConcurrentModificationException so we must make a quite unusual copy here
            //we can not only use one of the List.copyOf(), ArrayList(...) and so on e.g. they often just copy the data but a concurrent copy can lead to data corruption or null values
            try {
                List<Artifact> copyList = new ArrayList<>();
                for (Iterator<Artifact> iterator = list.iterator(); iterator.hasNext();) {
                    Artifact a = iterator.next();
                    if (a != null) {
                        copyList.add(a);
                    }
                }
                return copyList;
            } catch (ConcurrentModificationException e) {
                //retry...
                Thread.yield();
            }
        }

    }

    void addPomDependencyConsumer(Consumer<PomDependency> consumer) {
        gatheredDependencies.forEach(consumer);
        dependencyConsumer.add(consumer);
    }

}
