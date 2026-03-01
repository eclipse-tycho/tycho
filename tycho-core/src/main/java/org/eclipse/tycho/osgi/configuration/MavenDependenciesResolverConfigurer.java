/*******************************************************************************
 * Copyright (c) 2020, 2022 Christoph Läubrich and others.
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
package org.eclipse.tycho.osgi.configuration;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.RepositorySessionDecorator;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.tycho.MavenArtifactRepositoryReference;
import org.eclipse.tycho.core.MavenDependenciesResolver;
import org.eclipse.tycho.core.MavenModelFacade;
import org.eclipse.tycho.core.maven.MavenArtifactFacade;

@Named
@Singleton
public class MavenDependenciesResolverConfigurer implements MavenDependenciesResolver {

    @Inject
    private Logger logger;

    @Inject
    private LegacySupport context;

    @Inject
    private RepositorySystem repositorySystem;

    @Inject
    private List<RepositorySessionDecorator> decorators;

    @Override
    public Collection<?> resolve(String groupId, String artifactId, String version, String packaging, String classifier,
            Collection<String> scopes, int depth, Collection<MavenArtifactRepositoryReference> additionalRepositories,
            Object session) throws org.eclipse.tycho.core.DependencyResolutionException {
        
        String extension = packaging != null ? packaging : "jar";
        org.eclipse.aether.artifact.Artifact aetherArtifact;
        if (classifier != null && !classifier.isEmpty()) {
            aetherArtifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version);
        } else {
            aetherArtifact = new DefaultArtifact(groupId, artifactId, null, extension, version);
        }
        logger.debug("Resolving " + aetherArtifact);
        
        MavenSession mavenSession = getMavenSession(session);
        MavenProject project = mavenSession.getCurrentProject();
        RepositorySystemSession repositorySession = getRepositorySession(project, mavenSession);
        
        List<RemoteRepository> repositories = getEffectiveRepositories(project, additionalRepositories);
        
        try {
            if (depth == 0) {
                // Only resolve the root artifact without dependencies
                ArtifactRequest artifactRequest = new ArtifactRequest(aetherArtifact, repositories, null);
                org.eclipse.aether.resolution.ArtifactResult result = repositorySystem.resolveArtifact(repositorySession, artifactRequest);
                Artifact mavenArtifact = RepositoryUtils.toArtifact(result.getArtifact());
                if (mavenArtifact != null && mavenArtifact.getFile() != null) {
                    return List.of(new MavenArtifactFacade(mavenArtifact));
                }
                return List.of();
            }
            
            // Collect and resolve dependencies
            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(new Dependency(aetherArtifact, null));
            collectRequest.setRepositories(repositories);
            
            CollectResult collectResult = repositorySystem.collectDependencies(repositorySession, collectRequest);
            DependencyNode rootNode = collectResult.getRoot();
            
            final int maxDepth = depth;
            final Collection<String> effectiveScopes = scopes;
            
            DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, (node, parents) -> {
                // Filter by depth (parents list size represents the depth)
                if (parents.size() >= maxDepth) {
                    return false;
                }
                // Filter by scope
                Dependency dependency = node.getDependency();
                if (dependency != null) {
                    Artifact mavenArt = RepositoryUtils.toArtifact(dependency.getArtifact());
                    if (mavenArt != null) {
                        mavenArt.setScope(dependency.getScope());
                        return isValidScope(mavenArt, effectiveScopes);
                    }
                }
                return true;
            });
            dependencyRequest.setRoot(rootNode);
            
            DependencyResult dependencyResult = repositorySystem.resolveDependencies(repositorySession, dependencyRequest);
            
            List<Object> result = new ArrayList<>();
            for (org.eclipse.aether.resolution.ArtifactResult ar : dependencyResult.getArtifactResults()) {
                if (ar.isResolved()) {
                    org.eclipse.aether.artifact.Artifact resolved = ar.getArtifact();
                    if (resolved != null && resolved.getFile() != null) {
                        Artifact mavenArtifact = RepositoryUtils.toArtifact(resolved);
                        DependencyNode node = ar.getRequest().getDependencyNode();
                        if (node != null && node.getDependency() != null) {
                            mavenArtifact.setScope(node.getDependency().getScope());
                        }
                        if (isValidScope(mavenArtifact, effectiveScopes)) {
                            result.add(new MavenArtifactFacade(mavenArtifact));
                        }
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Failed to resolve: " + ar.getRequest().getArtifact());
                    }
                }
            }
            return result;
            
        } catch (ArtifactResolutionException | DependencyCollectionException | DependencyResolutionException e) {
            throw new org.eclipse.tycho.core.DependencyResolutionException("resolving " + aetherArtifact + " failed!", List.of(e));
        }
    }

    private RepositorySystemSession getRepositorySession(MavenProject project, MavenSession session) {
        RepositorySystemSession repositorySession = session.getRepositorySession();
        for (RepositorySessionDecorator decorator : decorators) {
            RepositorySystemSession decorated = decorator.decorate(project, repositorySession);
            if (decorated != null) {
                repositorySession = decorated;
            }
        }
        return repositorySession;
    }

    public static List<RemoteRepository> getEffectiveRepositories(MavenProject project,
            Collection<MavenArtifactRepositoryReference> additionalRepositories) {
        List<RemoteRepository> repositories = new ArrayList<>();
        if (project == null) {
            // Use Maven Central as default
            repositories.add(new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build());
        } else {
            List<RemoteRepository> projectRepos = project.getRemoteProjectRepositories();
            if (projectRepos != null) {
                repositories.addAll(projectRepos);
            }
        }
        
        if (additionalRepositories != null && !additionalRepositories.isEmpty()) {
            for (MavenArtifactRepositoryReference reference : additionalRepositories) {
                repositories.add(new RemoteRepository.Builder(reference.getId(), "default", reference.getUrl()).build());
            }
        }
        
        return repositories;
    }

    protected boolean isValidScope(Artifact artifact, Collection<String> scopes) {
        String artifactScope = artifact.getScope();
        if (artifactScope == null || artifactScope.isBlank()) {
            return true;
        }
        //compile is the default scope if not specified see
        // https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#dependency-scope
        if (scopes == null || scopes.isEmpty()) {
            return Artifact.SCOPE_COMPILE.equalsIgnoreCase(artifactScope);
        }
        for (String scope : scopes) {
            if (artifactScope.equals(scope)) {
                return true;
            }
        }
        //invalid scope type
        return false;
    }

    protected MavenSession getMavenSession(Object session) {
        return session instanceof MavenSession mavenSession //
                ? mavenSession
                : Objects.requireNonNull(context.getSession(),
                        "Can't acquire maven session from context, called outside maven thread context?");
    }

    @Override
    public File getRepositoryRoot() {
        return new File(getMavenSession(null).getLocalRepository().getBasedir());
    }

    @Override
    public MavenModelFacade loadModel(File modelFile) throws IOException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model;
        try (FileReader fileReader = new FileReader(modelFile, StandardCharsets.UTF_8)) {
            model = reader.read(fileReader);
        } catch (XmlPullParserException e) {
            throw new IOException("can't read model", e);
        }
        return new MavenModelFacade() {

            @Override
            public String getUrl() {
                return model.getUrl();
            }

            @Override
            public String getDescription() {
                return model.getDescription();
            }

            @Override
            public String getName() {
                return model.getName();
            }

            @Override
            public String getGroupId() {
                String groupId = model.getGroupId();
                if (groupId == null || groupId.isBlank()) {
                    Parent parent = model.getParent();
                    if (parent != null) {
                        return parent.getGroupId();
                    }
                }
                return groupId;
            }

            @Override
            public String getArtifactId() {
                return model.getArtifactId();
            }

            @Override
            public String getVersion() {
                String version = model.getVersion();
                if (version == null || version.isBlank()) {
                    Parent parent = model.getParent();
                    if (parent != null) {
                        return parent.getVersion();
                    }
                }
                return version;
            }

            @Override
            public String getPackaging() {
                return model.getPackaging();
            }

            @Override
            public Stream<MavenLicense> getLicenses() {
                return model.getLicenses().stream().map(l -> new MavenLicense() {

                    @Override
                    public String getName() {
                        return l.getName();
                    }

                    @Override
                    public String getUrl() {
                        return l.getUrl();
                    }

                    @Override
                    public String getComments() {
                        return l.getComments();
                    }
                });
            }
        };
    }

}
