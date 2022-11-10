/*******************************************************************************
 * Copyright (c) 2011, 2022 SAP SE and others.
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
 *                          - [Bug 533747] Target file is read and parsed over and over again
 *                          - [Bug 568729] Support new "Maven" Target location
 *                          - [Bug 569060] All ids of target file must be different
 *                          - [Bug 569481] Support for maven target location includeSource="true" attribute
 *                          - [Issue #401] Support nested targets
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.eclipse.tycho.BuildFailureException;
import org.eclipse.tycho.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.resolver.target.TargetDefinitionContent;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenDependenciesResolver;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.core.shared.MultiLineLogger;
import org.eclipse.tycho.p2.repository.ListCompositeArtifactRepository;
import org.eclipse.tycho.p2.repository.ListCompositeMetadataRepository;
import org.eclipse.tycho.p2.resolver.FileTargetDefinitionContent;
import org.eclipse.tycho.p2.resolver.MavenTargetDefinitionContent;
import org.eclipse.tycho.p2.resolver.ResolverException;
import org.eclipse.tycho.p2.resolver.URITargetDefinitionContent;
import org.eclipse.tycho.repository.util.LoggingProgressMonitor;
import org.eclipse.tycho.targetplatform.TargetDefinition;
import org.eclipse.tycho.targetplatform.TargetDefinition.DirectoryLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.FeaturesLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.Location;
import org.eclipse.tycho.targetplatform.TargetDefinition.MavenGAVLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.PathLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.ProfileLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.Repository;
import org.eclipse.tycho.targetplatform.TargetDefinition.TargetReferenceLocation;
import org.eclipse.tycho.targetplatform.TargetDefinitionFile;
import org.eclipse.tycho.targetplatform.TargetDefinitionResolutionException;
import org.eclipse.tycho.targetplatform.TargetDefinitionSyntaxException;

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
    private MavenDependenciesResolver mavenDependenciesResolver;

    public TargetDefinitionResolver(List<TargetEnvironment> environments,
            ExecutionEnvironmentResolutionHints executionEnvironment, IncludeSourceMode includeSourceMode,
            MavenContext mavenContext, MavenDependenciesResolver mavenDependenciesResolver) {
        this.environments = environments;
        this.executionEnvironment = executionEnvironment;
        this.includeSourceMode = includeSourceMode;
        this.mavenContext = mavenContext;
        this.mavenDependenciesResolver = mavenDependenciesResolver;
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

    public TargetDefinitionContent resolveContentWithExceptions(TargetDefinition definition,
            IProvisioningAgent provisioningAgent)
            throws TargetDefinitionSyntaxException, TargetDefinitionResolutionException, ResolverException {
        Collector<IInstallableUnit> unitResultSet = new Collector<>();
        InstallableUnitResolver installableUnitResolver = null;
        Map<String, FileTargetDefinitionContent> fileRepositories = new LinkedHashMap<>();
        Map<String, URITargetDefinitionContent> uriRepositories = new LinkedHashMap<>();
        List<MavenTargetDefinitionContent> mavenLocations = new ArrayList<>();
        List<TargetDefinitionContent> referencedTargetLocations = new ArrayList<>();
        for (Location locationDefinition : definition.getLocations()) {
            if (locationDefinition instanceof InstallableUnitLocation installableUnitLocation) {
                if (installableUnitResolver == null) {
                    installableUnitResolver = new InstallableUnitResolver(environments, executionEnvironment,
                            includeSourceMode, logger);
                }
                List<URITargetDefinitionContent> locations = new ArrayList<>();
                for (Repository repository : installableUnitLocation.getRepositories()) {
                    URI location = repository.getLocation();
                    String key = location.normalize().toASCIIString();
                    locations.add(uriRepositories.computeIfAbsent(key,
                            s -> new URITargetDefinitionContent(provisioningAgent, location, repository.getId())));
                }
                IQueryable<IInstallableUnit> locationUnits = QueryUtil.compoundQueryable(locations);
                installableUnitResolver.addLocation((InstallableUnitLocation) locationDefinition, locationUnits);
            } else if (locationDefinition instanceof PathLocation pathLocation) {
                String resolvePath = resolvePath(pathLocation.getPath(), definition);
                File fileLocation;
                try {
                    fileLocation = new File(resolvePath).getCanonicalFile();
                } catch (IOException e) {
                    throw new ResolverException("I/O Error while resolving path " + resolvePath, e);
                }
                if (fileLocation.exists()) {
                    FileTargetDefinitionContent fileRepositoryRolver = fileRepositories.computeIfAbsent(
                            fileLocation.getAbsolutePath(),
                            key -> new FileTargetDefinitionContent(provisioningAgent, fileLocation));
                    if (pathLocation instanceof DirectoryLocation || pathLocation instanceof ProfileLocation) {
                        unitResultSet.addAll(
                                fileRepositoryRolver.query(QueryUtil.ALL_UNITS, new LoggingProgressMonitor(logger)));
                    } else if (pathLocation instanceof FeaturesLocation featuresLocation) {
                        IArtifactKey key = org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction
                                .createFeatureArtifactKey(featuresLocation.getId(), featuresLocation.getVersion());
                        unitResultSet.addAll(fileRepositoryRolver.query(QueryUtil.createIUQuery(key),
                                new LoggingProgressMonitor(logger)));
                    }
                } else {
                    logger.warn("Target location path '" + fileLocation.getAbsolutePath()
                            + "' does not exist, target resolution might be incomplete.");
                }
            } else if (locationDefinition instanceof MavenGAVLocation location) {
                MavenTargetDefinitionContent targetDefinitionContent = new MavenTargetDefinitionContent(location,
                        mavenDependenciesResolver, includeSourceMode, provisioningAgent, mavenContext);
                mavenLocations.add(targetDefinitionContent);
                IQueryResult<IInstallableUnit> result = targetDefinitionContent.query(QueryUtil.ALL_UNITS,
                        new LoggingProgressMonitor(logger));
                unitResultSet.addAll(result);
                if (logger.isDebugEnabled()) {
                    logger.debug("The following artifacts where resolved from location " + location);
                    for (IInstallableUnit iu : result.toUnmodifiableSet()) {
                        logger.debug("\t" + iu);
                    }
                }
            } else if (locationDefinition instanceof TargetReferenceLocation referenceLocation) {
                logger.info("Resolving " + referenceLocation.getUri());
                String resolvePath = resolvePath(referenceLocation.getUri(), definition);
                URI resolvedUri;
                try {
                    resolvedUri = new URI(convertRawToUri(resolvePath));
                } catch (URISyntaxException e) {
                    throw new ResolverException("Invalid URI " + resolvePath + ": " + e.getMessage(), e);
                }
                logger.info("Reading target " + resolvedUri + "...");
                TargetDefinitionContent content = resolveContentWithExceptions(TargetDefinitionFile.read(resolvedUri),
                        provisioningAgent);
                IQueryResult<IInstallableUnit> result = content.query(QueryUtil.ALL_UNITS,
                        new LoggingProgressMonitor(logger));
                unitResultSet.addAll(result);
                referencedTargetLocations.add(content);
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
        //preliminary step : add all maven locations and make the installable unit resolver aware of it
        for (MavenTargetDefinitionContent mavenContent : mavenLocations) {
            metadataRepositories.add(mavenContent.getMetadataRepository());
            artifactRepositories.add(mavenContent.getArtifactRepository());
        }
        //preliminary step: add all referenced targets:
        for (TargetDefinitionContent referenceContent : referencedTargetLocations) {
            metadataRepositories.add(referenceContent.getMetadataRepository());
            artifactRepositories.add(referenceContent.getArtifactRepository());
        }
        //now we can resolve the p2 sources
        if (installableUnitResolver != null) {
            //FIXME installableUnitResolver should provide Meta+Artifact repositories so we have a complete view on the target!
            IMetadataRepository metadataRepository = new ListCompositeMetadataRepository(metadataRepositories,
                    provisioningAgent);
            unitResultSet.addAll(installableUnitResolver.resolve(metadataRepository));
        }
        return new TargetDefinitionContent() {

            private ListCompositeArtifactRepository artifactRepository;

            private IMetadataRepository metadataRepository;

            @Override
            public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
                return unitResultSet.query(query, monitor);
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

    public static String convertRawToUri(String resolvePath) {
        //We need to convert windows path separators here...
        resolvePath = resolvePath.replace('\\', '/');
        String lc = resolvePath.toLowerCase();
        if (lc.startsWith("file:") && !lc.startsWith("file:/")) {
            //according to rfc a file URI must always start with a slash
            resolvePath = resolvePath.replaceFirst("(?i)^file:", "file:/");
        }
        return resolvePath;
    }

    protected String resolvePath(String path, TargetDefinition definition) {
        path = resolvePattern(path, SYSTEM_PROPERTY_PATTERN,
                key -> mavenContext.getSessionProperties().getProperty(key, ""));
        path = resolvePattern(path, ENV_VAR_PATTERN, key -> {
            String env = System.getenv(key);
            return env == null ? "" : env;
        });
        path = resolvePattern(path, PROJECT_LOC_PATTERN, this::findProjectLocation);
        return path;
    }

    private String findProjectLocation(String projectName) {
        if (projectName.startsWith("/")) {
            projectName = projectName.substring(1);
        }
        logger.debug("Find project location for project " + projectName);
        for (ReactorProject project : mavenContext.getProjects()) {
            String name = project.getName();
            logger.debug("check reactor project name: " + name);
            if (name.equals(projectName)) {
                return project.getBasedir().getAbsolutePath();
            }
        }
        for (ReactorProject project : mavenContext.getProjects()) {
            String artifactId = project.getArtifactId();
            logger.debug("check reactor project artifact id: " + artifactId);
            if (artifactId.equals(projectName)) {
                return project.getBasedir().getAbsolutePath();
            }
        }
        for (ReactorProject project : mavenContext.getProjects()) {
            String name = project.getBasedir().getName();
            logger.debug("check reactor project base directory: " + name);
            if (name.equals(projectName)) {
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
            String resolved = parameterResolver.apply(group);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(resolved));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static Pattern createVariablePatternArgument(String variableName) {
        return Pattern.compile("\\$\\{" + variableName + ":([^}]+)\\}", Pattern.CASE_INSENSITIVE);
    }

}
