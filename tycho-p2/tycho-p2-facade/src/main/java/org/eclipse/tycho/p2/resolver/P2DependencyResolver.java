/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype Inc. and others.
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
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.ProjectDependenciesResolver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
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
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.IDependencyMetadata;
import org.eclipse.tycho.IDependencyMetadata.DependencyMetadataType;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.artifacts.IllegalArtifactReferenceException;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.DependencyResolver;
import org.eclipse.tycho.core.DependencyResolverConfiguration;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TargetPlatformConfiguration.PomDependencies;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.ee.TargetDefinitionFile;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.maven.MavenDependencyInjector;
import org.eclipse.tycho.core.maven.utils.PluginRealmHelper;
import org.eclipse.tycho.core.osgitools.AbstractTychoProject;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DebugUtils;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.targetplatform.DefaultDependencyArtifacts;
import org.eclipse.tycho.core.osgitools.targetplatform.MultiEnvironmentDependencyArtifacts;
import org.eclipse.tycho.core.p2.P2ArtifactRepositoryLayout;
import org.eclipse.tycho.core.resolver.shared.MavenRepositoryLocation;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;
import org.eclipse.tycho.core.shared.BuildFailureException;
import org.eclipse.tycho.core.shared.BuildProperties;
import org.eclipse.tycho.core.shared.BuildPropertiesParser;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.osgi.adapters.MavenLoggerAdapter;
import org.eclipse.tycho.p2.facade.internal.AttachedArtifact;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.PublisherOptions;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.repository.registry.facade.ReactorRepositoryManagerFacade;

@Component(role = DependencyResolver.class, hint = P2DependencyResolver.ROLE_HINT, instantiationStrategy = "per-lookup")
public class P2DependencyResolver extends AbstractLogEnabled implements DependencyResolver, Initializable {

    public static final String ROLE_HINT = "p2";

    @Requirement
    private EquinoxServiceFactory equinox;

    @Requirement
    private BundleReader bundleReader;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private BuildPropertiesParser buildPropertiesParser;

    @Requirement
    private ProjectDependenciesResolver projectDependenciesResolver;

    @Requirement(role = TychoProject.class)
    private Map<String, TychoProject> projectTypes;

    @Requirement
    private PlexusContainer plexus;

    @Requirement
    private PluginRealmHelper pluginRealmHelper;

    private P2ResolverFactory resolverFactory;

    private DependencyMetadataGenerator generator;

    private ReactorRepositoryManagerFacade reactorRepositoryManager;

    @Override
    public void setupProjects(final MavenSession session, final MavenProject project,
            final ReactorProject reactorProject) {
        TargetPlatformConfiguration configuration = (TargetPlatformConfiguration) reactorProject
                .getContextValue(TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION);
        List<TargetEnvironment> environments = configuration.getEnvironments();
        Map<String, IDependencyMetadata> metadataMap = getDependencyMetadata(session, project, environments,
                OptionalResolutionAction.OPTIONAL);
        Map<DependencyMetadataType, Set<Object>> typeMap = new TreeMap<>();
        for (DependencyMetadataType type : DependencyMetadataType.values()) {
            typeMap.put(type, new LinkedHashSet<Object>());
        }
        for (IDependencyMetadata metadata : metadataMap.values()) {
            for (Entry<DependencyMetadataType, Set<Object>> map : typeMap.entrySet()) {
                map.getValue().addAll(metadata.getDependencyMetadata(map.getKey()));
            }
        }
        for (Entry<DependencyMetadataType, Set<Object>> entry : typeMap.entrySet()) {
            reactorProject.setDependencyMetadata(entry.getKey(), entry.getValue());
        }
    }

    protected Map<String, IDependencyMetadata> getDependencyMetadata(final MavenSession session,
            final MavenProject project, final List<TargetEnvironment> environments,
            final OptionalResolutionAction optionalAction) {

        final Map<String, IDependencyMetadata> metadata = new LinkedHashMap<>();
        metadata.put(null, generator.generateMetadata(new AttachedArtifact(project, project.getBasedir(), null),
                environments, optionalAction, new PublisherOptions()));

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
        } catch (MavenExecutionException e) {
            throw new RuntimeException(e);
        }

        return metadata;
    }

    protected boolean isTychoP2Plugin(PluginDescriptor pluginDescriptor) {
        if (pluginDescriptor.getArtifactMap().containsKey("org.eclipse.tycho:tycho-p2-facade")) {
            return true;
        }
        for (ComponentDependency dependency : pluginDescriptor.getDependencies()) {
            if ("org.eclipse.tycho".equals(dependency.getGroupId())
                    && "tycho-p2-facade".equals(dependency.getArtifactId())) {
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
        tpConfiguration.setIncludePackedArtifacts(configuration.isIncludePackedArtifacts());

        PomDependencyCollector pomDependencies = collectPomDependencies(project, reactorProjects, session,
                configuration.getPomDependencies());
        for (ArtifactRepository repository : project.getRemoteArtifactRepositories()) {
            addEntireP2RepositoryToTargetPlatform(repository, tpConfiguration);
        }

        tpConfiguration.setEnvironments(configuration.getEnvironments());
        for (File file : configuration.getTargets()) {
            addTargetFileContentToTargetPlatform(file, tpConfiguration);
        }

        tpConfiguration.addFilters(configuration.getFilters());
        tpConfiguration.setIncludeSourceMode(configuration.getTargetDefinitionIncludeSourceMode());

        return reactorRepositoryManager.computePreliminaryTargetPlatform(reactorProject, tpConfiguration, ee,
                reactorProjects, pomDependencies);
    }

    private ReactorProject getThisReactorProject(MavenSession session, MavenProject project,
            TargetPlatformConfiguration configuration) {
        // 'this' project should obey optionalDependencies configuration

        final List<TargetEnvironment> environments = configuration.getEnvironments();
        final OptionalResolutionAction optionalAction = configuration.getDependencyResolverConfiguration()
                .getOptionalResolutionAction();
        Map<String, IDependencyMetadata> dependencyMetadata = getDependencyMetadata(session, project, environments,
                optionalAction);
        Map<DependencyMetadataType, Set<Object>> typeMap = new TreeMap<>();
        for (Map.Entry<String, IDependencyMetadata> entry : dependencyMetadata.entrySet()) {
            IDependencyMetadata value = entry.getValue();
            for (DependencyMetadataType type : DependencyMetadataType.values()) {
                typeMap.computeIfAbsent(type, t -> new LinkedHashSet<>()).addAll(value.getDependencyMetadata(type));
            }
        }
        ReactorProject reactorProjet = new DefaultReactorProject(project) {
            @Override
            public Set<?> getDependencyMetadata(DependencyMetadataType type) {
                return typeMap.get(type);
            }
        };
        return reactorProjet;
    }

    private PomDependencyCollector collectPomDependencies(MavenProject project, List<ReactorProject> reactorProjects,
            MavenSession session, PomDependencies pomDependencies) {
        if (pomDependencies == PomDependencies.ignore) {
            return resolverFactory.newPomDependencyCollector(DefaultReactorProject.adapt(project));
        }

        Set<String> projectIds = new HashSet<>();
        for (ReactorProject p : reactorProjects) {
            String key = ArtifactUtils.key(p.getGroupId(), p.getArtifactId(), p.getVersion());
            projectIds.add(key);
        }

        ArrayList<String> scopes = new ArrayList<>();
        scopes.add(Artifact.SCOPE_COMPILE);
        Collection<Artifact> artifacts;
        try {
            artifacts = projectDependenciesResolver.resolve(project, scopes, session);
        } catch (MultipleArtifactsNotFoundException e) {
            Collection<Artifact> missing = new HashSet<>(e.getMissingArtifacts());

            for (Iterator<Artifact> it = missing.iterator(); it.hasNext();) {
                Artifact a = it.next();
                String key = ArtifactUtils.key(a.getGroupId(), a.getArtifactId(), a.getBaseVersion());
                if (projectIds.contains(key)) {
                    it.remove();
                }
            }

            if (!missing.isEmpty()) {
                throw new RuntimeException("Could not resolve project dependencies", e);
            }

            artifacts = e.getResolvedArtifacts();
            artifacts.removeAll(e.getMissingArtifacts());
        } catch (AbstractArtifactResolutionException e) {
            throw new RuntimeException("Could not resolve project dependencies", e);
        }
        List<Artifact> externalArtifacts = new ArrayList<>(artifacts.size());
        for (Artifact artifact : artifacts) {
            String key = ArtifactUtils.key(artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion());
            if (projectIds.contains(key)) {
                // resolved to an older snapshot from the repo, we only want the current project in the reactor
                continue;
            }
            externalArtifacts.add(artifact);
        }
        PomDependencyProcessor pomDependencyProcessor = new PomDependencyProcessor(session, repositorySystem,
                resolverFactory, equinox.getService(LocalRepositoryP2Indices.class), getLogger());
        return pomDependencyProcessor.collectPomDependencies(project, externalArtifacts,
                pomDependencies == PomDependencies.wrapAsBundle);
    }

    private void addEntireP2RepositoryToTargetPlatform(ArtifactRepository repository,
            TargetPlatformConfigurationStub resolutionContext) {
        try {
            if (repository.getLayout() instanceof P2ArtifactRepositoryLayout) {
                URI url = new URL(repository.getUrl()).toURI();
                resolutionContext.addP2Repository(new MavenRepositoryLocation(repository.getId(), url));

                getLogger().debug("Added p2 repository " + repository.getId() + " (" + repository.getUrl() + ")");
            }
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException("Invalid repository URL: " + repository.getUrl(), e);
        }
    }

    private void addTargetFileContentToTargetPlatform(File targetFile,
            TargetPlatformConfigurationStub resolutionContext) {
        TargetDefinitionFile target = TargetDefinitionFile.read(targetFile);
        resolutionContext.addTargetDefinition(target);
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

        for (ReactorProject otherProject : reactorProjects) {
            projects.put(otherProject.getBasedir(), otherProject);
        }

        if (resolverConfiguration != null) {
            for (Dependency dependency : resolverConfiguration.getExtraRequirements()) {
                try {
                    resolver.addDependency(dependency.getType(), dependency.getArtifactId(), dependency.getVersion());
                } catch (IllegalArtifactReferenceException e) {
                    throw new BuildFailureException("Invalid extraRequirement " + dependency.getType() + ":"
                            + dependency.getArtifactId() + ":" + dependency.getVersion() + ": " + e.getMessage(), e);
                }
            }
        }

        BuildProperties buildProperties = buildPropertiesParser.parse(project.getBasedir());
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
            P2ResolutionResult result = resolver.collectProjectDependencies(targetPlatform,
                    optionalDependencyPreparedProject);

            return newDefaultTargetPlatform(DefaultReactorProject.adapt(project), projects, result);
        }
    }

    private boolean isAllowConflictingDependencies(MavenProject project, TargetPlatformConfiguration configuration) {
        String packaging = project.getPackaging();

        if (PackagingType.TYPE_ECLIPSE_UPDATE_SITE.equals(packaging)
                || PackagingType.TYPE_ECLIPSE_FEATURE.equals(packaging)) {
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
        return platform;
    }

    @Override
    public void initialize() throws InitializationException {
        this.resolverFactory = equinox.getService(P2ResolverFactory.class);
        this.generator = equinox.getService(DependencyMetadataGenerator.class, "(role-hint=dependency-only)");
        this.reactorRepositoryManager = equinox.getService(ReactorRepositoryManagerFacade.class);
    }

    @Override
    public void injectDependenciesIntoMavenModel(MavenProject project, AbstractTychoProject projectType,
            DependencyArtifacts dependencyArtifacts, DependencyArtifacts testDependencyArtifacts, Logger logger) {
        MavenDependencyInjector.injectMavenDependencies(project, dependencyArtifacts, testDependencyArtifacts,
                bundleReader, logger);
    }
}
