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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.ComponentDependency;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.eclipse.equinox.p2.core.spi.IAgentService;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.BuildFailureException;
import org.eclipse.tycho.BuildProperties;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.IDependencyMetadata;
import org.eclipse.tycho.IDependencyMetadata.DependencyMetadataType;
import org.eclipse.tycho.IllegalArtifactReferenceException;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.DependencyResolver;
import org.eclipse.tycho.core.DependencyResolverConfiguration;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.maven.MavenDependencyInjector;
import org.eclipse.tycho.core.osgitools.AbstractTychoProject;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DebugUtils;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.targetplatform.DefaultDependencyArtifacts;
import org.eclipse.tycho.core.osgitools.targetplatform.MultiEnvironmentDependencyArtifacts;
import org.eclipse.tycho.core.resolver.P2ResolutionResult;
import org.eclipse.tycho.core.resolver.P2Resolver;
import org.eclipse.tycho.core.resolver.P2ResolverFactory;
import org.eclipse.tycho.core.resolver.shared.PomDependencies;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.osgi.TychoServiceFactory;
import org.eclipse.tycho.osgi.adapters.MavenLoggerAdapter;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.PublisherOptions;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.p2maven.helper.PluginRealmHelper;
import org.eclipse.tycho.p2maven.repository.P2ArtifactRepositoryLayout;
import org.eclipse.tycho.repository.registry.facade.ReactorRepositoryManager;
import org.eclipse.tycho.targetplatform.TargetDefinitionFile;

@Component(role = DependencyResolver.class, hint = P2DependencyResolver.ROLE_HINT, instantiationStrategy = "per-lookup")
public class P2DependencyResolver extends AbstractLogEnabled implements DependencyResolver, Initializable {

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

    @Requirement(hint = TychoServiceFactory.HINT)
    private EquinoxServiceFactory serviceFactory;

    @Requirement
    private PomUnits pomUnits;

    @Override
    public void setupProjects(final MavenSession session, final MavenProject project,
            final ReactorProject reactorProject) {
        TargetPlatformConfiguration configuration = (TargetPlatformConfiguration) reactorProject
                .getContextValue(TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION);
        List<TargetEnvironment> environments = configuration.getEnvironments();
        Map<String, IDependencyMetadata> metadataMap = getDependencyMetadata(session, project, environments,
                OptionalResolutionAction.OPTIONAL);
        Map<DependencyMetadataType, Set<IInstallableUnit>> typeMap = new TreeMap<>();
        for (DependencyMetadataType type : DependencyMetadataType.values()) {
            typeMap.put(type, new LinkedHashSet<>());
        }
        for (IDependencyMetadata metadata : metadataMap.values()) {
            typeMap.forEach((key, value) -> value.addAll(metadata.getDependencyMetadata(key)));
        }
        Set<IInstallableUnit> initial = new HashSet<>();
        typeMap.forEach((key, value) -> {
            reactorProject.setDependencyMetadata(key, value);
            initial.addAll(value);
        });
        reactorProject.setDependencyMetadata(DependencyMetadataType.INITIAL, initial);
    }

    protected Map<String, IDependencyMetadata> getDependencyMetadata(final MavenSession session,
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
            pluginRealmHelper.execute(session, project, () -> {
                try {
                    for (P2MetadataProvider provider : plexus.lookupList(P2MetadataProvider.class)) {
                        Map<String, IDependencyMetadata> providedMetadata = provider.getDependencyMetadata(session,
                                project, null, optionalAction);
                        if (providedMetadata != null) {
                            metadata.putAll(providedMetadata);
                        }
                    }
                } catch (ComponentLookupException e) {
                    // have not found anything
                }
            }, this::isTychoP2Plugin);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return metadata;
    }

    protected boolean isTychoP2Plugin(PluginDescriptor pluginDescriptor) {
        if (pluginDescriptor.getArtifactMap().containsKey("org.eclipse.tycho:tycho-core")) {
            return true;
        }
        for (ComponentDependency dependency : pluginDescriptor.getDependencies()) {
            if ("org.eclipse.tycho".equals(dependency.getGroupId())
                    && "tycho-core".equals(dependency.getArtifactId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public TargetPlatform computePreliminaryTargetPlatform(MavenSession session, MavenProject project,
            List<ReactorProject> reactorProjects) {
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        TargetPlatformConfiguration configuration = TychoProjectUtils.getTargetPlatformConfiguration(reactorProject);
        ExecutionEnvironmentConfiguration ee = TychoProjectUtils.getExecutionEnvironmentConfiguration(reactorProject);

        TargetPlatformConfigurationStub tpConfiguration = new TargetPlatformConfigurationStub();
        for (ArtifactRepository repository : project.getRemoteArtifactRepositories()) {
            addEntireP2RepositoryToTargetPlatform(repository, tpConfiguration);
        }

        tpConfiguration.setEnvironments(configuration.getEnvironments());
        for (TargetDefinitionFile target : configuration.getTargets()) {
            tpConfiguration.addTargetDefinition(target);
        }

        tpConfiguration.addFilters(configuration.getFilters());
        tpConfiguration.setIncludeSourceMode(configuration.getTargetDefinitionIncludeSourceMode());

        return reactorRepositoryManager.computePreliminaryTargetPlatform(reactorProject, tpConfiguration, ee,
                reactorProjects);
    }

    private ReactorProject getThisReactorProject(MavenSession session, MavenProject project,
            TargetPlatformConfiguration configuration) {
        // 'this' project should obey optionalDependencies configuration

        final List<TargetEnvironment> environments = configuration.getEnvironments();
        final OptionalResolutionAction optionalAction = configuration.getDependencyResolverConfiguration()
                .getOptionalResolutionAction();
        Map<String, IDependencyMetadata> dependencyMetadata = getDependencyMetadata(session, project, environments,
                optionalAction);
        Map<DependencyMetadataType, Set<IInstallableUnit>> typeMap = new TreeMap<>();
        for (IDependencyMetadata value : dependencyMetadata.values()) {
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
        TargetPlatformConfiguration configuration = TychoProjectUtils.getTargetPlatformConfiguration(reactorProject);
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
            serviceFactory.getService(IAgentService.class); //this will force triggering service loadings that are required to resolve URLs!
            if (repository.getLayout() instanceof P2ArtifactRepositoryLayout) {
                //TODO we might pass an URLStreamHandler here that collects handlers from plexus components?
                URI url = new URL(repository.getUrl()).toURI();
                resolutionContext.addP2Repository(new MavenRepositoryLocation(repository.getId(), url));

                getLogger().debug("Added p2 repository " + repository.getId() + " (" + repository.getUrl() + ")");
            }
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException("Invalid repository URL: " + repository.getUrl(), e);
        }
    }

    @Override
    public DependencyArtifacts resolveDependencies(final MavenSession session, final MavenProject project,
            TargetPlatform targetPlatform, List<ReactorProject> reactorProjects,
            DependencyResolverConfiguration resolverConfiguration) {
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        if (targetPlatform == null) {
            targetPlatform = TychoProjectUtils.getTargetPlatform(reactorProject);
        }

        // TODO 364134 For compatibility reasons, target-platform-configuration includes settings for the dependency resolution
        // --> split this information logically, e.g. through two distinct interfaces
        TargetPlatformConfiguration configuration = TychoProjectUtils.getTargetPlatformConfiguration(reactorProject);

        P2Resolver osgiResolverImpl = resolverFactory
                .createResolver(new MavenLoggerAdapter(getLogger(), DebugUtils.isDebugEnabled(session, project)));

        return doResolveDependencies(session, project, reactorProjects, resolverConfiguration, targetPlatform,
                osgiResolverImpl, configuration);
    }

    private DependencyArtifacts doResolveDependencies(MavenSession session, MavenProject project,
            List<ReactorProject> reactorProjects, DependencyResolverConfiguration resolverConfiguration,
            TargetPlatform targetPlatform, P2Resolver resolver, TargetPlatformConfiguration configuration) {

        Map<File, ReactorProject> projects = new HashMap<>();

        resolver.setEnvironments(configuration.getEnvironments());
        resolver.setAdditionalFilterProperties(configuration.getProfileProperties());
        resolver.setPomDependencies(configuration.getPomDependencies());

        for (ReactorProject otherProject : reactorProjects) {
            projects.put(otherProject.getBasedir(), otherProject);
        }

        if (resolverConfiguration != null) {
            for (ArtifactKey dependency : resolverConfiguration.getExtraRequirements()) {
                try {
                    resolver.addDependency(dependency.getType(), dependency.getId(), dependency.getVersion());
                } catch (IllegalArtifactReferenceException e) {
                    throw new BuildFailureException("Invalid extraRequirement " + dependency.getType() + ":"
                            + dependency.getId() + ":" + dependency.getVersion() + ": " + e.getMessage(), e);
                }
            }
        }

        BuildProperties buildProperties = DefaultReactorProject.adapt(project).getBuildProperties();
        Collection<String> additionalBundles = buildProperties.getAdditionalBundles();
        for (String additionalBundle : additionalBundles) {
            resolver.addAdditionalBundleDependency(additionalBundle);
        }
        // get reactor project with prepared optional dependencies // TODO use original IU and have the resolver create the modified IUs
        ReactorProject optionalDependencyPreparedProject = getThisReactorProject(session, project, configuration);

        if (!isAllowConflictingDependencies(project, configuration)) {
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
        } else {
            //FIXME this reference to removed update site, check if we can remove this!
            P2ResolutionResult result = resolver.collectProjectDependencies(targetPlatform,
                    optionalDependencyPreparedProject);

            return newDefaultTargetPlatform(DefaultReactorProject.adapt(project), projects, result);
        }
    }

    private boolean isAllowConflictingDependencies(MavenProject project, TargetPlatformConfiguration configuration) {
        String packaging = project.getPackaging();

        if (PackagingType.TYPE_ECLIPSE_FEATURE.equals(packaging)) {
            Boolean allow = configuration.getAllowConflictingDependencies();
            if (allow != null) {
                return allow.booleanValue();
            }
        }

        // conflicting dependencies do not make sense for products and bundles
        return false;
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
    public void injectDependenciesIntoMavenModel(MavenProject project, AbstractTychoProject projectType,
            DependencyArtifacts dependencyArtifacts, DependencyArtifacts testDependencyArtifacts, Logger logger) {
        MavenDependencyInjector.injectMavenDependencies(project, dependencyArtifacts, testDependencyArtifacts,
                bundleReader, resolverFactory::resolveDependencyDescriptor, logger, repositorySystem,
                context.getSession().getSettings());
    }
}
