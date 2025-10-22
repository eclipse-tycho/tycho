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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.tycho.MavenArtifactRepositoryReference;
import org.eclipse.tycho.core.DependencyResolutionException;
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

    @Override
    public Collection<?> resolve(String groupId, String artifactId, String version, String packaging, String classifier,
            Collection<String> scopes, int depth, Collection<MavenArtifactRepositoryReference> additionalRepositories,
            Object session) throws DependencyResolutionException {
        Artifact artifact;
        if (classifier != null && !classifier.isEmpty()) {
            artifact = repositorySystem.createArtifactWithClassifier(groupId, artifactId, version, packaging,
                    classifier);
        } else {
            artifact = repositorySystem.createArtifact(groupId, artifactId, version, null, packaging);
        }
        logger.debug("Resolving " + artifact);
        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifact);
        MavenSession mavenSession = getMavenSession(session);
        request.setResolveRoot(true);
        request.setOffline(mavenSession.isOffline());
        request.setCollectionFilter(a -> isValidScope(a, scopes));
        request.setResolutionFilter(a -> {
            List<String> trail = a.getDependencyTrail();
            if (logger.isDebugEnabled()) {
                logger.debug("[depth=" + trail.size() + ", scope matches =" + isValidScope(a, scopes) + "][" + a + "]["
                        + trail.stream().collect(Collectors.joining(" >> ")) + "]");
            }
            return trail.size() <= depth && isValidScope(a, scopes);
        });
        request.setLocalRepository(mavenSession.getLocalRepository());
        request.setResolveTransitively(depth > 0);
        request.setRemoteRepositories(getEffectiveRepositories(mavenSession.getCurrentProject(), additionalRepositories,
                repositorySystem, mavenSession.getSettings()));
        ArtifactResolutionResult result = repositorySystem.resolve(request);
        if (result.hasExceptions()) {
            throw new DependencyResolutionException("resolving " + artifact + " failed!", result.getExceptions());
        }
        return result.getArtifacts().stream().filter(a -> a.getFile() != null).map(MavenArtifactFacade::new).toList();
    }

    @SuppressWarnings("deprecation")
    public static List<ArtifactRepository> getEffectiveRepositories(MavenProject project,
            Collection<MavenArtifactRepositoryReference> additionalRepositories, RepositorySystem repositorySystem,
            Settings settings) {
        List<ArtifactRepository> projectRepositories;
        if (project == null) {
            try {
                projectRepositories = List.of(repositorySystem.createDefaultRemoteRepository());
            } catch (InvalidRepositoryException e) {
                projectRepositories = List.of();
            }
        } else {
            projectRepositories = project.getRemoteArtifactRepositories();
        }
        if (additionalRepositories != null && !additionalRepositories.isEmpty()) {
            List<ArtifactRepository> repositories = new ArrayList<>(projectRepositories);
            for (MavenArtifactRepositoryReference reference : additionalRepositories) {
                repositories.add(repositorySystem.createArtifactRepository(reference.getId(), reference.getUrl(), null,
                        null, null));
            }
            projectRepositories = repositorySystem.getEffectiveRepositories(repositories);
        }
        repositorySystem.injectMirror(projectRepositories, settings.getMirrors());
        repositorySystem.injectProxy(projectRepositories, settings.getProxies());
        repositorySystem.injectAuthentication(projectRepositories, settings.getServers());
        return projectRepositories;
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
