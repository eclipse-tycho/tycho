/*******************************************************************************
 * Copyright (c) 2011, 2020 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - [Bug 538144] Support other target locations (Directory, Features, Installations)
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.CollectionResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.shared.BuildFailureException;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.core.shared.MultiLineLogger;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.p2.remote.IRepositoryIdManager;
import org.eclipse.tycho.p2.resolver.FileRepositoryRolver;
import org.eclipse.tycho.p2.resolver.InstallableUnitResolver;
import org.eclipse.tycho.p2.resolver.Resolvable;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.DirectoryLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.FeaturesLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Location;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.PathLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.ProfileLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Repository;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionResolutionException;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionSyntaxException;
import org.eclipse.tycho.p2.util.resolution.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.p2.util.resolution.ResolverException;
import org.eclipse.tycho.repository.p2base.artifact.repository.LazyArtifactRepository;
import org.eclipse.tycho.repository.p2base.artifact.repository.ListCompositeArtifactRepository;
import org.eclipse.tycho.repository.p2base.artifact.repository.ListCompositeMetadataRepository;
import org.eclipse.tycho.repository.p2base.artifact.repository.RepositoryArtifactProvider;
import org.eclipse.tycho.repository.util.LoggingProgressMonitor;

/**
 * Class which performs target definition resolution. This class is used by the
 * {@link TargetDefinitionResolverService} instance.
 * 
 * @see TargetDefinitionResolverService
 */
public final class TargetDefinitionResolver {

    private static final Pattern SYSTEM_PROPERTY_PATTERN = createVariablePatternArgument("system_property");
    private static final Pattern PROJECT_LOC_PATTERN = createVariablePatternArgument("project_loc");
    private static final Pattern ENV_VAR_PATTERN = createVariablePatternArgument("env_var");

    private IMetadataRepositoryManager metadataManager;
    private IRepositoryIdManager repositoryIdManager;

    private final MavenLogger logger;

    private final List<TargetEnvironment> environments;

    private final ExecutionEnvironmentResolutionHints executionEnvironment;

    private MavenContext mavenContext;

    public TargetDefinitionResolver(List<TargetEnvironment> environments,
            ExecutionEnvironmentResolutionHints executionEnvironment, IProvisioningAgent agent,
            MavenContext mavenContext) {
        this.environments = environments;
        this.executionEnvironment = executionEnvironment;
        this.mavenContext = mavenContext;
        this.logger = mavenContext.getLogger();
        this.metadataManager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
        this.repositoryIdManager = (IRepositoryIdManager) agent.getService(IRepositoryIdManager.SERVICE_NAME);
    }

    public TargetDefinitionContent resolveContent(TargetDefinition definition, IProvisioningAgent provisioningAgent) {
        try {
            return resolveContentWithExceptions(definition, provisioningAgent);
        } catch (TargetDefinitionSyntaxException e) {
            throw new BuildFailureException(
                    "Invalid syntax in target definition " + definition.getOrigin() + ": " + e.getMessage(), e);
        } catch (TargetDefinitionResolutionException e) {
            throw new BuildFailureException(
                    "Failed to resolve target definition " + definition.getOrigin() + ": " + e.getMessage(), e);
        } catch (ResolverException e) {
            logResolverException(e);
            throw new BuildFailureException("Failed to resolve target definition " + definition.getOrigin(), e);
        }
    }

    private void logResolverException(ResolverException e) {
        logger.error("Cannot resolve target definition:");
        new MultiLineLogger(logger).error(e.getDetails(), "  ");
        logger.error("");
    }

    TargetDefinitionContent resolveContentWithExceptions(TargetDefinition definition,
            IProvisioningAgent provisioningAgent)
            throws TargetDefinitionSyntaxException, TargetDefinitionResolutionException, ResolverException {
        List<Resolvable> resolveSources = new ArrayList<>();
        List<URI> p2ArtifactRepositoryURLs = new ArrayList<>();
        InstallableUnitResolver installableUnitResolver = null;
        Map<String, FileRepositoryRolver> fileRepositories = new LinkedHashMap<>();
        for (Location locationDefinition : definition.getLocations()) {
            if (locationDefinition instanceof InstallableUnitLocation) {
                if (installableUnitResolver == null) {
                    installableUnitResolver = new InstallableUnitResolver(metadataManager, repositoryIdManager,
                            environments, executionEnvironment, logger);
                    resolveSources.add(installableUnitResolver);
                }
                installableUnitResolver.addLocation((InstallableUnitLocation) locationDefinition);

                for (Repository repository : ((InstallableUnitLocation) locationDefinition).getRepositories()) {
                    p2ArtifactRepositoryURLs.add(repository.getLocation());
                }
            } else if (locationDefinition instanceof PathLocation) {
                PathLocation pathLocation = (PathLocation) locationDefinition;
                File path = resolvePath(pathLocation.getPath(), definition);
                if (path.exists()) {
                    FileRepositoryRolver fileRepositoryRolver = fileRepositories.computeIfAbsent(path.getAbsolutePath(),
                            key -> new FileRepositoryRolver(provisioningAgent, path));
                    if (pathLocation instanceof DirectoryLocation || pathLocation instanceof ProfileLocation) {
                        resolveSources.add(monitor -> fileRepositoryRolver.getUnits());
                    } else if (pathLocation instanceof FeaturesLocation) {
                        FeaturesLocation featuresLocation = (FeaturesLocation) pathLocation;
                        @SuppressWarnings("restriction")
                        IArtifactKey key = org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction
                                .createFeatureArtifactKey(featuresLocation.getId(), featuresLocation.getVersion());
                        resolveSources.add(monitor -> fileRepositoryRolver.getUnits(key));
                    }
                } else {
                    logger.warn("Target location path '" + path.getAbsolutePath()
                            + "' does not exits, target resoloution might be incomplete.");
                }
            } else {
                logger.warn("Target location type '" + locationDefinition.getTypeDescription() + "' is not supported");
            }
        }

        if (definition.hasIncludedBundles()) {
            // the bundle selection list is currently not taken into account (see bug 373776)
            logger.warn(
                    "De-selecting bundles in a target definition file is not supported. See https://wiki.eclipse.org/Tycho_Messages_Explained#Target_File_Include_Bundles for alternatives.");
        }

        List<IMetadataRepository> metadataRepositories = new ArrayList<>();
        List<IArtifactRepository> artifactRepositories = new ArrayList<>();
        //preliminary step : resolve all file locations, but don't add the units
        for (FileRepositoryRolver fileRepositoryRolver : fileRepositories.values()) {
            fileRepositoryRolver.resolve(new LoggingProgressMonitor(logger));
            if (installableUnitResolver != null) {
                installableUnitResolver.getAvailableUnitSources().add(fileRepositoryRolver.getMetadataRepository());
                metadataRepositories.add(fileRepositoryRolver.getMetadataRepository());
            }
            artifactRepositories.add(fileRepositoryRolver.getArtifactRepository());
        }
        if (p2ArtifactRepositoryURLs.size() > 0) {
            //artifact repositories are resolved lazy here as loading them might not be always necessary (e.g only dependency resolution required) and could be expensive (net I/O)
            for (URI uri : p2ArtifactRepositoryURLs) {
                artifactRepositories.add(
                        new LazyArtifactRepository(provisioningAgent, uri, RepositoryArtifactProvider::loadRepository));
            }
        }
        if (installableUnitResolver != null) {
            metadataRepositories.addAll(installableUnitResolver.getLoadedIULocations());
        }
        //now resolve the individual items of the target and add the necessary items
        Collection<IInstallableUnit> result = new HashSet<IInstallableUnit>();
        for (Resolvable source : resolveSources) {
            result.addAll(source.resolve(new LoggingProgressMonitor(logger)));
        }
        return new TargetDefinitionContent() {

            private IMetadataRepository metadataRepository;
            private ListCompositeArtifactRepository artifactRepository;

            @Override
            public IQueryable<IInstallableUnit> getUnits() {
                return new CollectionResult<>(result);
            }

            @Override
            public IMetadataRepository getMetadataRepository() {
                if (metadataRepository == null) {
                    metadataRepository = new ListCompositeMetadataRepository(metadataRepositories, provisioningAgent);
                }
                return metadataRepository;
            }

            @Override
            public IArtifactRepository getArtifactRepository() {
                if (artifactRepository == null) {
                    artifactRepository = new ListCompositeArtifactRepository(provisioningAgent, artifactRepositories);
                }
                return artifactRepository;
            }

        };
    }

    protected File resolvePath(String path, TargetDefinition definition) throws ResolverException {
        ReactorProject project = definition.getProject();
        if (project != null) {
            path = path.replace("${project_loc}", project.getBasedir().getAbsolutePath());
            path = path.replace("${project_name}", project.getName());
            path = path.replace("${project_path}", project.getBasedir().getAbsolutePath());
        }
        path = resolvePattern(path, SYSTEM_PROPERTY_PATTERN,
                key -> mavenContext.getSessionProperties().getProperty(key, ""));
        path = resolvePattern(path, ENV_VAR_PATTERN, key -> {
            String env = System.getenv(key);
            return env == null ? "" : env;
        });
        path = resolvePattern(path, PROJECT_LOC_PATTERN, this::findProjectLocation);
        try {
            return new File(path).getCanonicalFile();
        } catch (IOException e) {
            throw new ResolverException("I/O Error while resolve path " + path, e);
        }
    }

    private String findProjectLocation(String projectName) {
        for (ReactorProject project : mavenContext.getProjects()) {
            if (project.getName().equals(projectName)) {
                return project.getBasedir().getAbsolutePath();
            }
        }
        for (ReactorProject project : mavenContext.getProjects()) {
            if (project.getArtifactId().equals(projectName)) {
                return project.getBasedir().getAbsolutePath();
            }
        }
        for (ReactorProject project : mavenContext.getProjects()) {
            if (project.getBasedir().getName().equals(projectName)) {
                return project.getBasedir().getAbsolutePath();
            }
        }

        //if we can't resolve this, we will return the original one as this might be intentional to not include the project in the build
        String defaultValue = "${project_loc:" + projectName + "}";
        logger.warn("Can't resolve " + defaultValue + " target resoloution might be incomplete");
        return defaultValue;
    }

    private static String resolvePattern(String input, Pattern pattern, Function<String, String> parameterResolver) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(sb, parameterResolver.apply(group));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static Pattern createVariablePatternArgument(String variableName) {
        return Pattern.compile("\\$\\{" + variableName + ":([^}]+)\\}", Pattern.CASE_INSENSITIVE);
    }

}
