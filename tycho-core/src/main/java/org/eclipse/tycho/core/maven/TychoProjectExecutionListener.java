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
package org.eclipse.tycho.core.maven;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectExecutionEvent;
import org.apache.maven.execution.ProjectExecutionListener;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.DependencyResolutionException;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.osgitools.DefaultArtifactDescriptor;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.p2.resolver.ResolverException;
import org.eclipse.tycho.p2maven.DependencyChain;
import org.eclipse.tycho.p2maven.InstallableUnitGenerator;
import org.eclipse.tycho.resolver.TychoResolver;

@Named("tycho")
@Singleton
public class TychoProjectExecutionListener implements ProjectExecutionListener {

    @Inject
    private TychoResolver resolver;

    @Inject
    private ModelWriter modelWriter;

    @Inject
    private LegacySupport legacySupport;

    private Set<MavenProject> finished = ConcurrentHashMap.newKeySet();

    @Inject
    private Logger logger;

    @Inject
    private TychoProjectManager projectManager;

    @Inject
    private InstallableUnitGenerator generator;

    @Override
    public void beforeProjectExecution(ProjectExecutionEvent event) throws LifecycleExecutionException {

    }

    private boolean requiresDependencies(ProjectExecutionEvent event) {
        List<MojoExecution> executionPlan = event.getExecutionPlan();
        if (executionPlan == null) {
            //we can't know ...
            return true;
        }
        for (MojoExecution execution : executionPlan) {
            MojoDescriptor mojoDescriptor = execution.getMojoDescriptor();
            String dependencyResolutionRequired = mojoDescriptor.getDependencyResolutionRequired();
            if (dependencyResolutionRequired != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void beforeProjectLifecycleExecution(ProjectExecutionEvent event) throws LifecycleExecutionException {
        MavenProject mavenProject = event.getProject();
        TargetPlatformConfiguration configuration = projectManager.getTargetPlatformConfiguration(mavenProject);
        if (configuration.isRequireEagerResolve() || !requiresDependencies(event)) {
            return;
        }
        MavenSession mavenSession = event.getSession();
        MavenSession oldSession = legacySupport.getSession();
        try {
            legacySupport.setSession(mavenSession);
            //FIXME should return tycho project!
            try {
                resolver.resolveProject(mavenSession, mavenProject);
            } catch (DependencyResolutionException e) {
                ResolverException resolverException = ResolverException.findResolverException(e);
                if (resolverException == null) {
                    throw new LifecycleExecutionException(
                            "Cannot resolve dependencies of project " + mavenProject.getId(), null, mavenProject, e);
                } else {
                    throw new LifecycleExecutionException(
                            "Cannot resolve dependencies of project " + mavenProject.getId() + System.lineSeparator()
                                    + " with context " + resolverException.getSelectionContext()
                                    + System.lineSeparator() + resolverException.explanations()
                                            .map(exp -> "  " + exp.toString()).collect(Collectors.joining("\n")),
                            null, mavenProject, resolverException);
                }
            }
            TychoProject tychoProject = projectManager.getTychoProject(mavenProject).orElse(null);
            if (tychoProject != null) {
                try {
                    Set<MavenProject> unfinished = checkBuildState(tychoProject, mavenProject);
                    if (unfinished.size() > 0) {
                        logger.warn("The following implicit projects are not referenced: " + System.lineSeparator()
                                + unfinished.stream().map(MavenProject::getId)
                                        .collect(Collectors.joining(System.lineSeparator())));
                    }
                } catch (CoreException e) {
                    //can't check the build state then...
                }
            }
        } finally {
            legacySupport.setSession(oldSession);
        }
        if (TychoMavenLifecycleParticipant.DUMP_DATA) {
            try {
                modelWriter.write(new File(mavenProject.getBasedir(), "pom-model-final.xml"), Map.of(),
                        mavenProject.getModel());
            } catch (IOException e) {
            }
        }
    }

    private Set<MavenProject> checkBuildState(TychoProject tychoProject, MavenProject project) throws CoreException {
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        DependencyArtifacts artifacts = tychoProject.getDependencyArtifacts(reactorProject);
        Set<MavenProject> unfinishedProjects = new HashSet<>();
        for (ArtifactDescriptor artifact : artifacts.getArtifacts()) {
            MavenProject artifactMavenProject = getMavenProject(artifact);
            if (artifactMavenProject != null && project != artifactMavenProject
                    && !finished.contains(artifactMavenProject)) {
                unfinishedProjects.add(artifactMavenProject);
                Collection<IInstallableUnit> projectUnits = generator.getInstallableUnits(project,
                        legacySupport.getSession(), false);
                ArtifactKey key = tychoProject.getArtifactKey(reactorProject);
                ArtifactDescriptor root = new DefaultArtifactDescriptor(key, project.getBasedir(), reactorProject, null,
                        projectUnits);
                DependencyChain chain = new DependencyChain(root, artifacts.getArtifacts());
                List<ArtifactDescriptor> pathToRoot = chain.pathToRoot(artifact);
                String dependencyChain = pathToRoot.stream().map(descriptor -> {
                    String message = String.valueOf(descriptor.getKey());
                    if (descriptor.getMavenProject() == null) {
                        message += " (target dependency)";
                    } else {
                        message += " (reactor project)";
                    }
                    return message;
                }).collect(Collectors.joining(" --> "));
                String targetRequires = pathToRoot.stream().filter(d -> d.getMavenProject() == null)
                        .map(ArtifactDescriptor::getKey).map(String::valueOf).collect(Collectors.joining(", "));
                logger.warn("Your build is not self-contained! Project " + project.getId()
                        + " depends implicitly on reactor project " + artifactMavenProject.getId()
                        + " through target requirements " + targetRequires
                        + " that are not part of the reactor, offending dependency chain is " + dependencyChain);
            }
        }
        return unfinishedProjects;
    }

    private MavenProject getMavenProject(ArtifactDescriptor artifact) {
        ReactorProject reactorProject = artifact.getMavenProject();
        if (reactorProject != null) {
            return reactorProject.adapt(MavenProject.class);
        }
        return null;
    }

    @Override
    public void afterProjectExecutionSuccess(ProjectExecutionEvent event) throws LifecycleExecutionException {
        finished.add(event.getProject());
    }

    @Override
    public void afterProjectExecutionFailure(ProjectExecutionEvent event) {
        finished.add(event.getProject());
    }

}
