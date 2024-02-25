/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich    - Bug 551739, Bug 538144, Bug 533747
 *                          - [Bug 567098] pomDependencies=consider should wrap non-osgi jars
 *                          - [Bug 572481] Tycho does not understand "additional.bundles" directive in build.properties
 *                          - [Issue #462] Delay Pom considered items to the final Target Platform calculation 
 *                          - [Issue #626] Classpath computation must take fragments into account 
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.BuildFailureException;
import org.eclipse.tycho.BuildProperties;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.IDependencyMetadata;
import org.eclipse.tycho.IDependencyMetadata.DependencyMetadataType;
import org.eclipse.tycho.IllegalArtifactReferenceException;
import org.eclipse.tycho.MavenDependencyDescriptor;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.DependencyResolver;
import org.eclipse.tycho.core.DependencyResolverConfiguration;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TargetPlatformConfiguration.InjectP2MavenMetadataHandling;
import org.eclipse.tycho.core.TargetPlatformConfiguration.LocalArtifactHandling;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.maven.MavenDependenciesResolver;
import org.eclipse.tycho.core.maven.MavenDependencyInjector;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.targetplatform.DefaultDependencyArtifacts;
import org.eclipse.tycho.core.osgitools.targetplatform.MultiEnvironmentDependencyArtifacts;
import org.eclipse.tycho.core.resolver.AdditionalBundleRequirementsInstallableUnitProvider;
import org.eclipse.tycho.core.resolver.P2ResolutionResult;
import org.eclipse.tycho.core.resolver.P2Resolver;
import org.eclipse.tycho.core.resolver.P2ResolverFactory;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.resolver.shared.PomDependencies;
import org.eclipse.tycho.helper.PluginRealmHelper;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.PublisherOptions;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.p2.target.facade.TargetPlatformFactory;
import org.eclipse.tycho.p2maven.repository.P2ArtifactRepositoryLayout;
import org.eclipse.tycho.repository.registry.facade.ReactorRepositoryManager;
import org.eclipse.tycho.resolver.P2MetadataProvider;
import org.eclipse.tycho.targetplatform.TargetDefinitionFile;
import org.eclipse.tycho.targetplatform.TargetPlatformArtifactResolver;
import org.eclipse.tycho.targetplatform.TargetResolveException;

@Component(role = DependencyResolver.class, hint = P2DependencyResolver.ROLE_HINT, instantiationStrategy = "per-lookup")
public class P2DependencyResolver implements DependencyResolver, Initializable {

    public static final String ROLE_HINT = "p2";

    @Requirement
    private BundleReader bundleReader;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private TychoProjectManager projectManager;

    @Requirement
    private PlexusContainer plexus;

    @Requirement
    private PluginRealmHelper pluginRealmHelper;

    @Requirement
    private LegacySupport context;

    @Requirement
    private P2ResolverFactory resolverFactory;

    @Requirement(hint = DependencyMetadataGenerator.DEPENDENCY_ONLY)
    private DependencyMetadataGenerator generator;

    @Requirement
    private ReactorRepositoryManager reactorRepositoryManager;

    @Requirement
    private LocalRepositoryP2Indices p2index;

    @Requirement
    private BuildPropertiesParser buildPropertiesParser;

    @Requirement
    private PomUnits pomUnits;

    @Requirement
    private MavenDependenciesResolver dependenciesResolver;

    @Requirement
    private TargetPlatformFactory tpFactory;

    @Requirement
    private Logger logger;

    @Override
    public void setupProjects(final MavenSession session, final MavenProject project,
            final ReactorProject reactorProject) {
        Set<IInstallableUnit> initial = new HashSet<>();
        if (PackagingType.TYPE_ECLIPSE_TARGET_DEFINITION.equals(project.getPackaging())) {
            //Target projects do not have any (initial) dependency metadata
        } else {
            TargetPlatformConfiguration configuration = projectManager.getTargetPlatformConfiguration(project);
            List<TargetEnvironment> environments = configuration.getEnvironments();
            Collection<IDependencyMetadata> metadataMap = getDependencyMetadata(session, project, environments,
                    OptionalResolutionAction.OPTIONAL);
            Map<DependencyMetadataType, Set<IInstallableUnit>> typeMap = new TreeMap<>();
            for (DependencyMetadataType type : DependencyMetadataType.values()) {
                typeMap.put(type, new LinkedHashSet<>());
            }
            for (IDependencyMetadata metadata : metadataMap) {
                typeMap.forEach((key, value) -> value.addAll(metadata.getDependencyMetadata(key)));
            }
            typeMap.forEach((key, value) -> {
                reactorProject.setDependencyMetadata(key, value);
                initial.addAll(value);
            });
        }
        reactorProject.setDependencyMetadata(DependencyMetadataType.INITIAL, initial);
    }

    protected Collection<IDependencyMetadata> getDependencyMetadata(final MavenSession session,
            final MavenProject project, final List<TargetEnvironment> environments,
            final OptionalResolutionAction optionalAction) {
        final ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        final File artifactLocation = (File) reactorProject
                .getContextValue(TychoConstants.CTX_METADATA_ARTIFACT_LOCATION);
        final File location = artifactLocation != null ? artifactLocation : project.getBasedir();
        final Map<String, IDependencyMetadata> metadata = new LinkedHashMap<>();
        metadata.put(null, generator.generateMetadata(new AttachedArtifact(project, location, null), environments,
                optionalAction, new PublisherOptions()));

        // let external providers contribute additional metadata
        try {
            pluginRealmHelper.visitPluginExtensions(project, session, P2MetadataProvider.class, provider -> {
                Map<String, IDependencyMetadata> providedMetadata = provider.getDependencyMetadata(session, project,
                        null, optionalAction);
                if (providedMetadata != null) {
                    metadata.putAll(providedMetadata);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return metadata.values();
    }

    @Override
    public TargetPlatform getPreliminaryTargetPlatform(MavenSession mavenSession, MavenProject mavenProject) {
        ReactorProject reactorProject = DefaultReactorProject.adapt(mavenProject);
        return reactorProject.computeContextValue(TargetPlatform.PRELIMINARY_TARGET_PLATFORM_KEY, () -> {
            logger.debug("Computing preliminary target platform for " + mavenProject);
            List<ReactorProject> reactorProjects = DefaultReactorProject.adapt(mavenSession);
            TargetPlatformConfiguration configuration = projectManager.getTargetPlatformConfiguration(mavenProject);
            TargetPlatformConfigurationStub tpConfiguration = new TargetPlatformConfigurationStub();
            ExecutionEnvironmentConfiguration ee = projectManager.getExecutionEnvironmentConfiguration(mavenProject);
            for (ArtifactRepository repository : mavenProject.getRemoteArtifactRepositories()) {
                addEntireP2RepositoryToTargetPlatform(repository, tpConfiguration);
            }
            tpConfiguration.setEnvironments(configuration.getEnvironments());
            tpConfiguration.addFilters(configuration.getFilters());
            tpConfiguration.setReferencedRepositoryMode(configuration.getReferencedRepositoryMode());
            if (PackagingType.TYPE_ECLIPSE_TARGET_DEFINITION.equals(mavenProject.getPackaging())) {
                //for target definition project itself we only want the main target to be considered
                try {
                    File targetFile = TargetPlatformArtifactResolver.getMainTargetFile(mavenProject);
                    TargetDefinitionFile targetDefinitionFile = TargetDefinitionFile.read(targetFile);
                    tpConfiguration.addTargetDefinition(targetDefinitionFile);
                } catch (TargetResolveException e) {
                    logger.warn("Can't read main target definition file from project " + mavenProject.getId(), e);
                }
                //also we always want to ignore sources
                tpConfiguration.setIncludeSourceMode(IncludeSourceMode.ignore);
                //and local artifacts
                tpConfiguration.setIgnoreLocalArtifacts(true);
            } else {
                for (TargetDefinitionFile target : configuration.getTargets()) {
                    tpConfiguration.addTargetDefinition(target);
                }
                tpConfiguration.setIncludeSourceMode(configuration.getTargetDefinitionIncludeSourceMode());
                tpConfiguration.setIgnoreLocalArtifacts(
                        configuration.getIgnoreLocalArtifacts() == LocalArtifactHandling.ignore);
            }
            return tpFactory.createTargetPlatform(tpConfiguration, ee, reactorProjects, reactorProject);
        });
    }

    private ReactorProject getThisReactorProject(MavenSession session, MavenProject project,
            TargetPlatformConfiguration configuration) {
        // 'this' project should obey optionalDependencies configuration

        final List<TargetEnvironment> environments = configuration.getEnvironments();
        final OptionalResolutionAction optionalAction = configuration.getDependencyResolverConfiguration()
                .getOptionalResolutionAction();
        Collection<IDependencyMetadata> dependencyMetadata = getDependencyMetadata(session, project, environments,
                optionalAction);
        Map<DependencyMetadataType, Set<IInstallableUnit>> typeMap = new TreeMap<>();
        for (IDependencyMetadata value : dependencyMetadata) {
            for (DependencyMetadataType type : DependencyMetadataType.values()) {
                typeMap.computeIfAbsent(type, t -> new LinkedHashSet<>()).addAll(value.getDependencyMetadata(type));
            }
        }
        return new DefaultReactorProject(project) {
            @Override
            public Set<IInstallableUnit> getDependencyMetadata(DependencyMetadataType type) {
                return typeMap.get(type);
            }

            @Override
            public Object getContextValue(String key) {
                Object value = super.getContextValue(key);
                if (value == null) {
                    return DefaultReactorProject.adapt(project).getContextValue(key);
                }
                return value;
            }

            @Override
            public <T> T computeContextValue(String key, Supplier<T> initalValueSupplier) {
                return DefaultReactorProject.adapt(project).computeContextValue(key, initalValueSupplier);
            }

            @Override
            public void setContextValue(String key, Object value) {
                super.setContextValue(key, value);
                DefaultReactorProject.adapt(project).setContextValue(key, value);
            }

        };
    }

    @Override
    public PomDependencyCollector resolvePomDependencies(MavenSession session, MavenProject project) {

        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        TargetPlatformConfiguration configuration = projectManager.getTargetPlatformConfiguration(project);
        PomDependencies pomDependencies = configuration.getPomDependencies();
        PomDependencyCollector collector = resolverFactory.newPomDependencyCollector(reactorProject);
        if (pomDependencies == PomDependencies.ignore) {
            return collector;
        }
        pomUnits.addCollectedUnits(collector, reactorProject);
        return collector;
    }

    private void addEntireP2RepositoryToTargetPlatform(ArtifactRepository repository,
            TargetPlatformConfigurationStub resolutionContext) {
        try {
            if (repository.getLayout() instanceof P2ArtifactRepositoryLayout) {
                URI url = new URI(TargetDefinitionResolver.convertRawToUri(repository.getUrl()));
                resolutionContext.addP2Repository(new MavenRepositoryLocation(repository.getId(), url));
                logger.debug("Added p2 repository " + repository.getId() + " (" + repository.getUrl() + ")");
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid repository URI: " + repository.getUrl(), e);
        }
    }

    @Override
    public DependencyArtifacts resolveDependencies(final MavenSession session, final MavenProject project,
            TargetPlatform targetPlatform, DependencyResolverConfiguration resolverConfiguration,
            List<TargetEnvironment> environments) {
        Objects.requireNonNull(targetPlatform);
        TargetPlatformConfiguration configuration = projectManager.getTargetPlatformConfiguration(project);

        P2Resolver osgiResolverImpl = resolverFactory.createResolver(environments);
        List<ReactorProject> reactorProjects = DefaultReactorProject.adapt(session);
        return doResolveDependencies(session, project, reactorProjects, resolverConfiguration, targetPlatform,
                osgiResolverImpl, configuration);
    }

    private DependencyArtifacts doResolveDependencies(MavenSession session, MavenProject project,
            List<ReactorProject> reactorProjects, DependencyResolverConfiguration resolverConfiguration,
            TargetPlatform targetPlatform, P2Resolver resolver, TargetPlatformConfiguration configuration) {

        Map<File, ReactorProject> projects = new HashMap<>();

        resolver.setAdditionalFilterProperties(configuration.getProfileProperties());
        resolver.setPomDependencies(configuration.getPomDependencies());

        for (ReactorProject otherProject : reactorProjects) {
            projects.put(otherProject.getBasedir(), otherProject);
        }

        if (resolverConfiguration != null) {
            for (ArtifactKey dependency : resolverConfiguration.getAdditionalArtifacts()) {
                try {
                    resolver.addDependency(dependency.getType(), dependency.getId(), dependency.getVersion());
                } catch (IllegalArtifactReferenceException e) {
                    throw new BuildFailureException("Invalid extraRequirement " + dependency.getType() + ":"
                            + dependency.getId() + ":" + dependency.getVersion() + ": " + e.getMessage(), e);
                }
            }
            for (IRequirement requirement : resolverConfiguration.getAdditionalRequirements()) {
                resolver.addRequirement(requirement);
            }
            Set<IInstallableUnit> additionalDependencyMetadata = DefaultReactorProject.adapt(project)
                    .getDependencyMetadata(DependencyMetadataType.ADDITIONAL);
            for (IInstallableUnit unit : additionalDependencyMetadata) {
                for (IRequirement requirement : unit.getRequirements()) {
                    resolver.addRequirement(requirement);
                }
            }
        }

        BuildProperties buildProperties = buildPropertiesParser.parse(DefaultReactorProject.adapt(project));
        Collection<String> additionalBundles = buildProperties.getAdditionalBundles();
        for (String additionalBundle : additionalBundles) {
            resolver.addAdditionalBundleDependency(additionalBundle);
        }
        projectManager.getBndTychoProject(project)
                .ifPresent(processor -> AdditionalBundleRequirementsInstallableUnitProvider
                        .getBndClasspathRequirements(processor).forEach(resolver::addRequirement));
        // get reactor project with prepared optional dependencies // TODO use original IU and have the resolver create the modified IUs
        ReactorProject optionalDependencyPreparedProject = getThisReactorProject(session, project, configuration);

        Map<TargetEnvironment, P2ResolutionResult> results = resolver.resolveTargetDependencies(targetPlatform,
                optionalDependencyPreparedProject);

        MultiEnvironmentDependencyArtifacts multiPlatform = new MultiEnvironmentDependencyArtifacts(
                DefaultReactorProject.adapt(project));

        for (Entry<TargetEnvironment, P2ResolutionResult> entry : results.entrySet()) {
            TargetEnvironment environment = entry.getKey();
            P2ResolutionResult result = entry.getValue();

            DefaultDependencyArtifacts platform = newDefaultTargetPlatform(DefaultReactorProject.adapt(project),
                    projects, result);

            multiPlatform.addPlatform(environment, platform);
        }

        return multiPlatform;
    }

    protected DefaultDependencyArtifacts newDefaultTargetPlatform(ReactorProject project,
            Map<File, ReactorProject> projects, P2ResolutionResult result) {
        DefaultDependencyArtifacts platform = new DefaultDependencyArtifacts(project);

        platform.addNonReactorUnits(result.getNonReactorUnits());

        for (P2ResolutionResult.Entry entry : result.getArtifacts()) {
            ArtifactKey key = new DefaultArtifactKey(entry.getType(), entry.getId(), entry.getVersion());
            ReactorProject otherProject = null;
            File location = entry.getLocation(false);
            if (location != null) {
                otherProject = projects.get(location);
            }
            if (otherProject != null) {
                platform.addReactorArtifact(key, otherProject, entry.getClassifier(), entry.getInstallableUnits());
            } else {
                platform.addArtifactFile(key, () -> entry.getLocation(true), entry.getInstallableUnits());
            }
        }
        for (P2ResolutionResult.Entry entry : result.getDependencyFragments()) {
            ArtifactKey key = new DefaultArtifactKey(entry.getType(), entry.getId(), entry.getVersion());
            platform.addFragment(key, () -> entry.getLocation(true), entry.getInstallableUnits());
        }
        return platform;
    }

    @Override
    public void initialize() throws InitializationException {
    }

    @Override
    public void injectDependenciesIntoMavenModel(MavenProject project, TychoProject projectType,
            DependencyArtifacts dependencyArtifacts, DependencyArtifacts testDependencyArtifacts, Logger logger) {
        Function<ArtifactDescriptor, MavenDependencyDescriptor> descriptorMapping;
        TargetPlatformConfiguration configuration = projectManager.getTargetPlatformConfiguration(project);
        if (configuration.getP2MetadataHandling() == InjectP2MavenMetadataHandling.inject) {
            descriptorMapping = resolverFactory::resolveDependencyDescriptor;
        } else if (configuration.getP2MetadataHandling() == InjectP2MavenMetadataHandling.validate) {
            descriptorMapping = descriptor -> resolveDescriptorWithValidation(project, logger, descriptor);
        } else {
            descriptorMapping = null;
        }
        MavenDependencyInjector.injectMavenDependencies(project, dependencyArtifacts, testDependencyArtifacts,
                bundleReader, descriptorMapping, logger, repositorySystem, context.getSession().getSettings(),
                buildPropertiesParser, configuration);
    }

    private MavenDependencyDescriptor resolveDescriptorWithValidation(MavenProject project, Logger logger,
            ArtifactDescriptor descriptor) {
        MavenDependencyDescriptor result = resolverFactory.resolveDependencyDescriptor(descriptor);
        if (MavenDependencyInjector.isValidMavenDescriptor(result)) {
            try {
                dependenciesResolver.resolveArtifact(project, context.getSession(), result.getGroupId(),
                        result.getArtifactId(), result.getVersion());
            } catch (ArtifactResolutionException e) {
                logger.warn("Mapping P2 > Maven Coordinates failed: " + e.getMessage());
                return null;
            }
        }
        return result;
    }

}
