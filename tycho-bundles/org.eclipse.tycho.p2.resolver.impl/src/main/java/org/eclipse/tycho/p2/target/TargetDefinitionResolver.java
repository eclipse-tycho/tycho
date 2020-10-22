/*******************************************************************************
 * Copyright (c) 2011, 2020 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich    - [Bug 538144] Support other target locations (Directory, Features, Installations)
 *                          - [Bug 533747] - Target file is read and parsed over and over again
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.shared.BuildFailureException;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.core.shared.MultiLineLogger;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.p2.resolver.FileTargetDefinitionContent;
import org.eclipse.tycho.p2.resolver.InstallableUnitResolver;
import org.eclipse.tycho.p2.resolver.URITargetDefinitionContent;
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
import org.eclipse.tycho.repository.p2base.artifact.repository.ListCompositeArtifactRepository;
import org.eclipse.tycho.repository.p2base.artifact.repository.ListCompositeMetadataRepository;
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

    private final MavenLogger logger;

    private final List<TargetEnvironment> environments;

    private final ExecutionEnvironmentResolutionHints executionEnvironment;

    private MavenContext mavenContext;
    private IncludeSourceMode includeSourceMode;

    public TargetDefinitionResolver(List<TargetEnvironment> environments,
            ExecutionEnvironmentResolutionHints executionEnvironment, IncludeSourceMode includeSourceMode,
            MavenContext mavenContext) {
        this.environments = environments;
        this.executionEnvironment = executionEnvironment;
        this.includeSourceMode = includeSourceMode;
        this.mavenContext = mavenContext;
        this.logger = mavenContext.getLogger();
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
        Collector<IInstallableUnit> unitResultSet = new Collector<>();
        InstallableUnitResolver installableUnitResolver = null;
        Map<String, FileTargetDefinitionContent> fileRepositories = new LinkedHashMap<>();
        Map<String, URITargetDefinitionContent> uriRepositories = new LinkedHashMap<>();
        for (Location locationDefinition : definition.getLocations()) {
            if (locationDefinition instanceof InstallableUnitLocation) {
                InstallableUnitLocation installableUnitLocation = (InstallableUnitLocation) locationDefinition;
                if (installableUnitResolver == null) {
                    installableUnitResolver = new InstallableUnitResolver(environments, executionEnvironment,
                            includeSourceMode, logger);
                }
                List<URITargetDefinitionContent> locations = new ArrayList<>();
                for (Repository repository : installableUnitLocation.getRepositories()) {
                    URI location = repository.getLocation();
                    String key;
                    String id = repository.getId();
                    if (id != null && !id.isBlank()) {
                        key = id;
                    } else {
                        key = location.normalize().toASCIIString();
                    }
                    locations.add(uriRepositories.computeIfAbsent(key,
                            s -> new URITargetDefinitionContent(provisioningAgent, location, id)));
                }
                IQueryable<IInstallableUnit> locationUnits = QueryUtil.compoundQueryable(locations);
                installableUnitResolver.addLocation((InstallableUnitLocation) locationDefinition, locationUnits);
            } else if (locationDefinition instanceof PathLocation) {
                PathLocation pathLocation = (PathLocation) locationDefinition;
                File path = resolvePath(pathLocation.getPath(), definition);
                if (path.exists()) {
                    FileTargetDefinitionContent fileRepositoryRolver = fileRepositories.computeIfAbsent(
                            path.getAbsolutePath(), key -> new FileTargetDefinitionContent(provisioningAgent, path));
                    if (pathLocation instanceof DirectoryLocation || pathLocation instanceof ProfileLocation) {
                        unitResultSet.addAll(
                                fileRepositoryRolver.query(QueryUtil.ALL_UNITS, new LoggingProgressMonitor(logger)));
                    } else if (pathLocation instanceof FeaturesLocation) {
                        //
                        FeaturesLocation featuresLocation = (FeaturesLocation) pathLocation;
                        @SuppressWarnings("restriction")
                        IArtifactKey key = org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction
                                .createFeatureArtifactKey(featuresLocation.getId(), featuresLocation.getVersion());
                        unitResultSet.addAll(fileRepositoryRolver.query(QueryUtil.createIUQuery(key),
                                new LoggingProgressMonitor(logger)));
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
        //preliminary step : add all file locations and make the installable unit resolver aware of it
        for (FileTargetDefinitionContent fileDefinitionContent : fileRepositories.values()) {
            metadataRepositories.add(fileDefinitionContent.getMetadataRepository());
            artifactRepositories.add(fileDefinitionContent.getArtifactRepository());
        }
        //preliminary step : add all file locations and make the installable unit resolver aware of it
        for (URITargetDefinitionContent uriDefinitionContent : uriRepositories.values()) {
            metadataRepositories.add(uriDefinitionContent.getMetadataRepository());
            artifactRepositories.add(uriDefinitionContent.getArtifactRepository());
        }
        //now we can resolve the p2 sources
        IMetadataRepository metadataRepository = new ListCompositeMetadataRepository(metadataRepositories,
                provisioningAgent);
        if (installableUnitResolver != null) {
            unitResultSet.addAll(installableUnitResolver.resolve(metadataRepository));
        }
        return new TargetDefinitionContent() {

            private IMetadataRepository metadataRepository;
            private ListCompositeArtifactRepository artifactRepository;

            @Override
            public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
                return unitResultSet.query(query, monitor);
            }

            @Override
            public IMetadataRepository getMetadataRepository() {
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
