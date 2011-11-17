/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver;

import java.io.File;
import java.io.IOException;
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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.ProjectDependenciesResolver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Server;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.TargetEnvironment;
import org.eclipse.tycho.core.TargetPlatform;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TargetPlatformResolver;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.maven.MavenDependencyInjector;
import org.eclipse.tycho.core.osgitools.AbstractTychoProject;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DefaultArtifactKey;
import org.eclipse.tycho.core.osgitools.targetplatform.AbstractTargetPlatformResolver;
import org.eclipse.tycho.core.osgitools.targetplatform.DefaultTargetPlatform;
import org.eclipse.tycho.core.osgitools.targetplatform.MultiEnvironmentTargetPlatform;
import org.eclipse.tycho.core.p2.P2ArtifactRepositoryLayout;
import org.eclipse.tycho.core.utils.ExecutionEnvironment;
import org.eclipse.tycho.core.utils.ExecutionEnvironmentUtils;
import org.eclipse.tycho.core.utils.PlatformPropertiesUtils;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.p2.facade.internal.ReactorArtifactFacade;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.resolver.facade.ResolutionContext;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Location;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionResolutionException;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionSyntaxException;

@Component(role = TargetPlatformResolver.class, hint = P2TargetPlatformResolver.ROLE_HINT, instantiationStrategy = "per-lookup")
public class P2TargetPlatformResolver extends AbstractTargetPlatformResolver implements TargetPlatformResolver,
        Initializable {

    public static final String ROLE_HINT = "p2";

    @Requirement
    private EquinoxServiceFactory equinox;

    @Requirement
    private BundleReader bundleReader;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement(hint = "p2")
    private ArtifactRepositoryLayout p2layout;

    @Requirement
    private ProjectDependenciesResolver projectDependenciesResolver;

    @Requirement(role = TychoProject.class)
    private Map<String, TychoProject> projectTypes;

    private P2ResolverFactory resolverFactory;

    private DependencyMetadataGenerator generator;

    private DependencyMetadataGenerator sourcesGenerator;

    private static final ArtifactRepositoryPolicy P2_REPOSITORY_POLICY = new ArtifactRepositoryPolicy(true,
            ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER, ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE);

    public void setupProjects(MavenSession session, MavenProject project, ReactorProject reactorProject) {
        TargetPlatformConfiguration configuration = (TargetPlatformConfiguration) project
                .getContextValue(TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION);
        List<Map<String, String>> environments = getEnvironments(configuration);
        Set<Object> metadata = generator
                .generateMetadata(new ReactorArtifactFacade(reactorProject, null), environments);
        reactorProject.setDependencyMetadata(null, metadata);

        // TODO this should be moved to osgi-sources-plugin somehow
        if (isBundleProject(project) && hasSourceBundle(project)) {
            ReactorArtifactFacade sourcesArtifact = new ReactorArtifactFacade(reactorProject, "sources");
            Set<Object> sourcesMetadata = sourcesGenerator.generateMetadata(sourcesArtifact, environments);
            reactorProject.setDependencyMetadata(sourcesArtifact.getClassidier(), sourcesMetadata);
        }
    }

    private static boolean isBundleProject(MavenProject project) {
        String type = project.getPackaging();
        return ArtifactKey.TYPE_ECLIPSE_PLUGIN.equals(type) || ArtifactKey.TYPE_ECLIPSE_TEST_PLUGIN.equals(type);
    }

    private static boolean hasSourceBundle(MavenProject project) {
        // TODO this is a fragile way of checking whether we generate a source bundle
        // should we rather use MavenSession to get the actual configured mojo instance?
        for (Plugin plugin : project.getBuildPlugins()) {
            if ("org.eclipse.tycho:tycho-source-plugin".equals(plugin.getKey())) {
                return true;
            }
        }
        return false;
    }

    public TargetPlatform resolvePlatform(final MavenSession session, final MavenProject project,
            List<ReactorProject> reactorProjects, List<Dependency> dependencies) {

        TargetPlatformConfiguration configuration = TychoProjectUtils.getTargetPlatformConfiguration(project);

        ExecutionEnvironment ee = projectTypes.get(project.getPackaging()).getExecutionEnvironment(project);

        ResolutionContext resolutionContext = resolverFactory.createResolutionContext(//
                ee != null ? ee.getProfileName() : null, configuration.isDisableP2Mirrors());

        P2Resolver osgiResolverImpl = resolverFactory.createResolver();

        try {
            return doResolvePlatform(session, project, reactorProjects, dependencies, resolutionContext,
                    osgiResolverImpl, configuration);
        } finally {
            resolutionContext.stop();
        }
    }

    protected TargetPlatform doResolvePlatform(final MavenSession session, final MavenProject project,
            List<ReactorProject> reactorProjects, List<Dependency> dependencies, ResolutionContext resolutionContext,
            P2Resolver resolver, TargetPlatformConfiguration configuration) {

        Map<File, ReactorProject> projects = new HashMap<File, ReactorProject>();

        resolver.setEnvironments(getEnvironments(configuration));

        for (ReactorProject otherProject : reactorProjects) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("P2resolver.addMavenProject " + otherProject.getId());
            }
            projects.put(otherProject.getBasedir(), otherProject);
            resolutionContext.addReactorArtifact(new ReactorArtifactFacade(otherProject, null));

            Map<String, Set<Object>> dependencyMetadata = otherProject.getDependencyMetadata();
            if (dependencyMetadata != null) {
                for (String classifier : dependencyMetadata.keySet()) {
                    resolutionContext.addReactorArtifact(new ReactorArtifactFacade(otherProject, classifier));
                }
            }
        }

        if (dependencies != null) {
            for (Dependency dependency : dependencies) {
                resolver.addDependency(dependency.getType(), dependency.getArtifactId(), dependency.getVersion());
            }
        }

        for (Dependency dependency : configuration.getExtraRequirements()) {
            resolver.addDependency(dependency.getType(), dependency.getArtifactId(), dependency.getVersion());
        }

        if (TargetPlatformConfiguration.POM_DEPENDENCIES_CONSIDER.equals(configuration.getPomDependencies())) {
            Set<String> projectIds = new HashSet<String>();
            for (ReactorProject p : reactorProjects) {
                String key = ArtifactUtils.key(p.getGroupId(), p.getArtifactId(), p.getVersion());
                projectIds.add(key);
            }

            ArrayList<String> scopes = new ArrayList<String>();
            scopes.add(Artifact.SCOPE_COMPILE);
            Collection<Artifact> artifacts;
            try {
                artifacts = projectDependenciesResolver.resolve(project, scopes, session);
            } catch (MultipleArtifactsNotFoundException e) {
                Collection<Artifact> missing = new HashSet<Artifact>(e.getMissingArtifacts());

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
            List<Artifact> externalArtifacts = new ArrayList<Artifact>(artifacts.size());
            for (Artifact artifact : artifacts) {
                String key = ArtifactUtils.key(artifact.getGroupId(), artifact.getArtifactId(),
                        artifact.getBaseVersion());
                if (projectIds.contains(key)) {
                    // resolved to an older snapshot from the repo, we only want the current project in the reactor
                    continue;
                }
                externalArtifacts.add(artifact);
            }
            List<Artifact> explicitArtifacts = MavenDependencyInjector.filterInjectedDependencies(externalArtifacts); // needed when the resolution is done again for the test runtime
            PomDependencyProcessor pomDependencyProcessor = new PomDependencyProcessor(session, repositorySystem,
                    getLogger());
            pomDependencyProcessor.addPomDependenciesToResolutionContext(project, explicitArtifacts, resolutionContext);
        }

        for (ArtifactRepository repository : project.getRemoteArtifactRepositories()) {
            try {
                URI uri = new URL(repository.getUrl()).toURI();

                if (repository.getLayout() instanceof P2ArtifactRepositoryLayout) {
                    if (session.isOffline()) {
                        getLogger().debug(
                                "Offline mode, using local cache only for repository " + repository.getId() + " ("
                                        + repository.getUrl() + ")");
                    }

                    try {
                        Authentication auth = repository.getAuthentication();
                        if (auth != null) {
                            resolutionContext.setCredentials(uri, auth.getUsername(), auth.getPassword());
                        }

                        resolutionContext.addP2Repository(uri);

                        getLogger().debug(
                                "Added p2 repository " + repository.getId() + " (" + repository.getUrl() + ")");
                    } catch (Exception e) {
                        String msg = "Failed to access p2 repository " + repository.getId() + " ("
                                + repository.getUrl() + "), will try to use local cache. Reason: " + e.getMessage();
                        if (getLogger().isDebugEnabled()) {
                            getLogger().warn(msg, e);
                        } else {
                            getLogger().warn(msg);
                        }
                    }
                }
            } catch (MalformedURLException e) {
                getLogger().warn("Could not parse repository URL", e);
            } catch (URISyntaxException e) {
                getLogger().warn("Could not parse repository URL", e);
            }
        }

        if (configuration.getTarget() != null) {
            final TargetDefinitionFile target;
            try {
                target = TargetDefinitionFile.read(configuration.getTarget());
            } catch (TargetDefinitionSyntaxException e) {
                throw new RuntimeException("Invalid syntax in target definition " + configuration.getTarget() + ": "
                        + e.getMessage(), e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Set<URI> uris = new HashSet<URI>();

            for (Location location : target.getLocations()) {
                if (!(location instanceof InstallableUnitLocation)) {
                    continue;
                }
                for (TargetDefinition.Repository repository : ((InstallableUnitLocation) location).getRepositories()) {

                    try {
                        URI uri = getMirror(repository, session.getRequest().getMirrors());
                        if (uris.add(uri)) {
                            if (!session.isOffline()) {
                                String id = repository.getId();
                                if (id != null) {
                                    Server server = session.getSettings().getServer(id);

                                    if (server != null) {
                                        // TODO don't do this via magic side-effects, but when loading repositories
                                        resolutionContext.setCredentials(uri, server.getUsername(),
                                                server.getPassword());
                                    } else {
                                        getLogger().info(
                                                "Unknown server id=" + id + " for repository location="
                                                        + repository.getLocation());
                                    }
                                }

                                // TODO mirrors are no longer considered -> lookup mirrors when loading p2 repositories
                            }
                        }
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }

                try {
                    resolutionContext.addTargetDefinition(target, getEnvironments(configuration));
                } catch (TargetDefinitionSyntaxException e) {
                    throw new RuntimeException("Invalid syntax in target definition " + configuration.getTarget()
                            + ": " + e.getMessage(), e);
                } catch (TargetDefinitionResolutionException e) {
                    throw new RuntimeException("Failed to resolve target definition " + configuration.getTarget(), e);
                }
            }
        }

        if (!isAllowConflictingDependencies(project, configuration)) {
            List<P2ResolutionResult> results = resolver.resolveProject(resolutionContext, project.getBasedir());

            MultiEnvironmentTargetPlatform multiPlatform = new MultiEnvironmentTargetPlatform();

            // FIXME this is just wrong
            for (int i = 0; i < configuration.getEnvironments().size(); i++) {
                TargetEnvironment environment = configuration.getEnvironments().get(i);
                P2ResolutionResult result = results.get(i);

                DefaultTargetPlatform platform = newDefaultTargetPlatform(session, projects, result);

                // addProjects( session, platform );

                multiPlatform.addPlatform(environment, platform);
            }

            return multiPlatform;
        } else {
            P2ResolutionResult result = resolver.collectProjectDependencies(resolutionContext, project.getBasedir());

            return newDefaultTargetPlatform(session, projects, result);
        }
    }

    private boolean isAllowConflictingDependencies(MavenProject project, TargetPlatformConfiguration configuration) {
        String packaging = project.getPackaging();

        if (org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_UPDATE_SITE.equals(packaging)
                || org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_FEATURE.equals(packaging)) {
            Boolean allow = configuration.getAllowConflictingDependencies();
            if (allow != null) {
                return allow.booleanValue();
            }
        }

        // conflicting dependencies do not make sense for products and bundles
        return false;
    }

    protected DefaultTargetPlatform newDefaultTargetPlatform(MavenSession session, Map<File, ReactorProject> projects,
            P2ResolutionResult result) {
        DefaultTargetPlatform platform = new DefaultTargetPlatform();

        platform.addSite(new File(session.getLocalRepository().getBasedir()));

        platform.addNonReactorUnits(result.getNonReactorUnits());

        for (P2ResolutionResult.Entry entry : result.getArtifacts()) {
            ArtifactKey key = new DefaultArtifactKey(entry.getType(), entry.getId(), entry.getVersion());
            ReactorProject otherProject = projects.get(entry.getLocation());
            if (otherProject != null) {
                platform.addReactorArtifact(key, otherProject, entry.getClassifier(), entry.getInstallableUnits());
            } else {
                platform.addArtifactFile(key, entry.getLocation(), entry.getInstallableUnits());
            }
        }
        return platform;
    }

    private List<Map<String, String>> getEnvironments(TargetPlatformConfiguration configuration) {
        ArrayList<Map<String, String>> environments = new ArrayList<Map<String, String>>();

        for (TargetEnvironment environment : configuration.getEnvironments()) {
            Properties properties = new Properties();
            properties.put(PlatformPropertiesUtils.OSGI_OS, environment.getOs());
            properties.put(PlatformPropertiesUtils.OSGI_WS, environment.getWs());
            properties.put(PlatformPropertiesUtils.OSGI_ARCH, environment.getArch());
            ExecutionEnvironmentUtils.loadVMProfile(properties);

            // TODO does not belong here
            properties.put("org.eclipse.update.install.features", "true");

            Map<String, String> map = new LinkedHashMap<String, String>();
            for (Object key : properties.keySet()) {
                map.put(key.toString(), properties.getProperty(key.toString()));
            }
            environments.add(map);
        }

        return environments;
    }

    private URI getMirror(TargetDefinition.Repository location, List<Mirror> mirrors) throws URISyntaxException {
        URI p2RepositoryLocation = location.getLocation();
        String id = location.getId();
        if (id == null) {
            id = p2RepositoryLocation.toString();
        }

        ArtifactRepository repository = repositorySystem.createArtifactRepository(id, p2RepositoryLocation.toString(),
                p2layout, P2_REPOSITORY_POLICY, P2_REPOSITORY_POLICY);

        Mirror mirror = repositorySystem.getMirror(repository, mirrors);

        return mirror != null ? new URI(mirror.getUrl()) : p2RepositoryLocation;
    }

    public void initialize() throws InitializationException {
        this.resolverFactory = equinox.getService(P2ResolverFactory.class);
        this.generator = equinox.getService(DependencyMetadataGenerator.class, "(role-hint=dependency-only)");
        this.sourcesGenerator = equinox.getService(DependencyMetadataGenerator.class, "(role-hint=source-bundle)");
    }

    public void injectDependenciesIntoMavenModel(MavenProject project, AbstractTychoProject projectType,
            TargetPlatform targetPlatform, Logger logger) {
        MavenDependencyInjector.injectMavenDependencies(project, targetPlatform, bundleReader, logger);
    }
}
