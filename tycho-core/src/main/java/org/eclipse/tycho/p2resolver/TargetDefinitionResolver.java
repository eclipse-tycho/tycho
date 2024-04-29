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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.updatesite.SiteCategory;
import org.eclipse.equinox.internal.p2.updatesite.SiteXMLAction;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.CollectionResult;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.BuildFailureException;
import org.eclipse.tycho.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.resolver.MavenTargetLocationFactory;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.resolver.shared.ReferencedRepositoryMode;
import org.eclipse.tycho.core.shared.LoggingProgressMonitor;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.core.shared.MultiLineLogger;
import org.eclipse.tycho.p2.repository.ListCompositeMetadataRepository;
import org.eclipse.tycho.p2.resolver.FileTargetDefinitionContent;
import org.eclipse.tycho.p2.resolver.ResolverException;
import org.eclipse.tycho.p2.resolver.URITargetDefinitionContent;
import org.eclipse.tycho.p2maven.ListCompositeArtifactRepository;
import org.eclipse.tycho.targetplatform.TargetDefinition;
import org.eclipse.tycho.targetplatform.TargetDefinition.DirectoryLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.FeaturesLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.FollowRepositoryReferences;
import org.eclipse.tycho.targetplatform.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.Location;
import org.eclipse.tycho.targetplatform.TargetDefinition.MavenGAVLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.PathLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.ProfileLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.Repository;
import org.eclipse.tycho.targetplatform.TargetDefinition.RepositoryLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.TargetReferenceLocation;
import org.eclipse.tycho.targetplatform.TargetDefinitionContent;
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

    private static final SiteXMLAction CATEGORY_FACTORY = new SiteXMLAction((URI) null, (String) null);

    private final MavenLogger logger;

    private final List<TargetEnvironment> environments;

    private final ExecutionEnvironmentResolutionHints executionEnvironment;

    private IncludeSourceMode includeSourceMode;
    private MavenTargetLocationFactory mavenDependenciesResolver;
    private TargetDefinitionVariableResolver varResolver;

    private ReferencedRepositoryMode referencedRepositoryMode;

    public TargetDefinitionResolver(List<TargetEnvironment> environments,
            ExecutionEnvironmentResolutionHints executionEnvironment, IncludeSourceMode includeSourceMode,
            ReferencedRepositoryMode referencedRepositoryMode, MavenContext mavenContext,
            MavenTargetLocationFactory mavenDependenciesResolver, TargetDefinitionVariableResolver varResolver) {
        this.environments = environments;
        this.executionEnvironment = executionEnvironment;
        this.includeSourceMode = includeSourceMode;
        this.referencedRepositoryMode = referencedRepositoryMode;
        this.mavenDependenciesResolver = mavenDependenciesResolver;
        this.logger = mavenContext.getLogger();
        this.varResolver = varResolver;
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
        List<TargetDefinitionContent> mavenLocations = new ArrayList<>();
        List<TargetDefinitionContent> referencedTargetLocations = new ArrayList<>();
        List<TargetDefinitionContent> repositoryLocations = new ArrayList<>();
        for (Location locationDefinition : definition.getLocations()) {
            if (locationDefinition instanceof InstallableUnitLocation installableUnitLocation) {
                if (installableUnitResolver == null) {
                    installableUnitResolver = new InstallableUnitResolver(environments, executionEnvironment,
                            includeSourceMode, logger);
                }
                List<URITargetDefinitionContent> locations = new ArrayList<>();
                var followRepositoryReferences = installableUnitLocation.followRepositoryReferences();
                final ReferencedRepositoryMode followReferences;
                if (followRepositoryReferences == FollowRepositoryReferences.DEFAULT) {
                    followReferences = referencedRepositoryMode;
                } else if (followRepositoryReferences == FollowRepositoryReferences.ENABLED) {
                    followReferences = ReferencedRepositoryMode.include;
                } else {
                    followReferences = ReferencedRepositoryMode.ignore;
                }
                for (Repository repository : installableUnitLocation.getRepositories()) {
                    URI location = resolveRepositoryLocation(repository.getLocation());
                    String key = location.normalize().toASCIIString();
                    locations.add(
                            uriRepositories.computeIfAbsent(key, s -> new URITargetDefinitionContent(provisioningAgent,
                                    location, repository.getId(), followReferences, logger)));
                }
                IQueryable<IInstallableUnit> locationUnits = QueryUtil.compoundQueryable(locations);
                Collection<IInstallableUnit> rootUnits = installableUnitResolver
                        .addLocation((InstallableUnitLocation) locationDefinition, locationUnits);
                unitResultSet.accept(
                        createCategory(installableUnitLocation.getRepositories().stream().map(r -> r.getLocation())
                                .collect(Collectors.joining(", ")), new CollectionResult<>(rootUnits)));
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
                    IQueryResult<IInstallableUnit> result;
                    if (pathLocation instanceof DirectoryLocation || pathLocation instanceof ProfileLocation) {
                        result = fileRepositoryRolver.query(QueryUtil.ALL_UNITS, new LoggingProgressMonitor(logger));
                    } else if (pathLocation instanceof FeaturesLocation featuresLocation) {
                        IArtifactKey key = org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction
                                .createFeatureArtifactKey(featuresLocation.getId(), featuresLocation.getVersion());
                        result = fileRepositoryRolver.query(QueryUtil.createIUQuery(key),
                                new LoggingProgressMonitor(logger));
                    } else {
                        continue;
                    }
                    unitResultSet.addAll(result);
                    unitResultSet.accept(createCategory(resolvePath, result));
                } else {
                    logger.warn("Target location path '" + fileLocation.getAbsolutePath()
                            + "' does not exist, target resolution might be incomplete.");
                }
            } else if (locationDefinition instanceof MavenGAVLocation mavenLocation) {
                TargetDefinitionContent targetDefinitionContent = mavenDependenciesResolver
                        .resolveTargetDefinitionContent(mavenLocation, includeSourceMode);
                mavenLocations.add(targetDefinitionContent);
                IQueryResult<IInstallableUnit> result = targetDefinitionContent.query(QueryUtil.ALL_UNITS,
                        new LoggingProgressMonitor(logger));
                unitResultSet.addAll(result);
                Set<IInstallableUnit> locationUnits = result.toUnmodifiableSet();
                if (logger.isDebugEnabled()) {
                    logger.debug("The following artifacts were resolved from location " + mavenLocation);
                    for (IInstallableUnit iu : locationUnits) {
                        logger.debug("\t" + iu);
                    }
                }
                unitResultSet.accept(createCategory(mavenLocation.getLabel(), result));
            } else if (locationDefinition instanceof TargetReferenceLocation referenceLocation) {
                logger.info("Resolving " + referenceLocation.getUri());
                String resolvePath = resolvePath(referenceLocation.getUri(), definition);
                URI resolvedUri;
                try {
                    resolvedUri = new URI(convertRawToUri(resolvePath));
                } catch (URISyntaxException e) {
                    throw new ResolverException("Invalid URI " + resolvePath + ": " + e.getMessage(), e);
                }
                logger.info("Reading target platform " + resolvedUri);
                TargetDefinitionContent content = resolveContentWithExceptions(TargetDefinitionFile.read(resolvedUri),
                        provisioningAgent);
                IQueryResult<IInstallableUnit> result = content.query(QueryUtil.ALL_UNITS,
                        new LoggingProgressMonitor(logger));
                unitResultSet.addAll(result);
                referencedTargetLocations.add(content);
            } else if (locationDefinition instanceof RepositoryLocation repositoryLocation) {
                URI resolvedUri;
                String uri = repositoryLocation.getUri();
                try {
                    resolvedUri = new URI(convertRawToUri(resolvePath(uri, definition)));
                } catch (URISyntaxException e) {
                    throw new ResolverException("Invalid URI " + resolvePath(uri, definition) + ": " + e.getMessage(),
                            e);
                }
                logger.info("Loading " + resolvedUri + "...");
                RepositoryLocationContent content = new RepositoryLocationContent(resolvedUri,
                        repositoryLocation.getRequirements(), provisioningAgent, logger);
                repositoryLocations.add(content);
                IQueryResult<IInstallableUnit> result = content.query(QueryUtil.ALL_UNITS,
                        new LoggingProgressMonitor(logger));
                unitResultSet.addAll(result);
                unitResultSet.accept(createCategory(uri, result));
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
        for (TargetDefinitionContent mavenContent : mavenLocations) {
            metadataRepositories.add(mavenContent.getMetadataRepository());
            artifactRepositories.add(mavenContent.getArtifactRepository());
        }
        //preliminary step: add all referenced targets:
        for (TargetDefinitionContent referenceContent : referencedTargetLocations) {
            metadataRepositories.add(referenceContent.getMetadataRepository());
            artifactRepositories.add(referenceContent.getArtifactRepository());
        }
        //preliminary step: add all repository locations:
        for (TargetDefinitionContent referenceContent : repositoryLocations) {
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
                    artifactRepository = new ListCompositeArtifactRepository(artifactRepositories, provisioningAgent);
                }
                return artifactRepository;
            }

        };
    }

    private static IInstallableUnit createCategory(String label, IQueryResult<IInstallableUnit> result) {
        SiteCategory category = new SiteCategory();
        category.setLabel(label);
        category.setName("generated.target.category." + UUID.randomUUID());
        return CATEGORY_FACTORY.createCategoryIU(category,
                result.stream().filter(iu -> !iu.getId().endsWith(".feature.jar")).collect(Collectors.toSet()));
    }

    /**
     * Converts a "raw" URI string into one that can be used to parse it as an {@link URI}. The
     * conversion is especially for converting file URIs constructed using maven properties that
     * otherwise can not easily be represented by the user as such for example
     * <code>file:${project.basedir}/my-repository</code> there is nothing much a user can do here.
     * Beside that, the method is <b>not</b> meant as a general purpose method to fix invalid URI
     * inputs!
     * 
     * 
     * @param raw
     *            the raw string
     * @return the converted URI representation
     */
    public static String convertRawToUri(String raw) {
        //We need to convert windows path separators here...
        raw = raw.replace('\\', '/');
        String lc = raw.toLowerCase();
        if (lc.startsWith("file:") && !lc.startsWith("file:/")) {
            //according to rfc a file URI must always start with a slash
            raw = raw.replaceFirst("(?i)^file:", "file:/");
        }
        return raw;
    }

    protected String resolvePath(String path, TargetDefinition definition) {
        return varResolver.resolve(path);
    }

    protected URI resolveRepositoryLocation(String location) {
        try {
            return new URI(varResolver.resolve(location));
        } catch (URISyntaxException e) {
            throw new TargetDefinitionSyntaxException("Invalid URI: " + location);
        }
    }

}
