/*******************************************************************************
 * Copyright (c) 2020, 2021 Christoph Läubrich and others.
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

import static org.apache.maven.artifact.Artifact.SCOPE_COMPILE;
import static org.apache.maven.artifact.Artifact.SCOPE_PROVIDED;
import static org.apache.maven.artifact.Artifact.SCOPE_RUNTIME;
import static org.apache.maven.artifact.Artifact.SCOPE_SYSTEM;
import static org.apache.maven.artifact.Artifact.SCOPE_TEST;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.eclipse.tycho.core.maven.MavenArtifactFacade;
import org.eclipse.tycho.core.shared.DependencyResolutionException;
import org.eclipse.tycho.core.shared.MavenArtifactRepositoryReference;
import org.eclipse.tycho.core.shared.MavenDependenciesResolver;
import org.eclipse.tycho.core.shared.MavenModelFacade;

@Component(role = EquinoxLifecycleListener.class, hint = "MavenDependenciesResolver")
public class MavenDependenciesResolverConfigurer extends EquinoxLifecycleListener implements MavenDependenciesResolver {

    @Requirement
    private Logger logger;

    @Requirement
    private LegacySupport context;

    @Requirement
    private RepositorySystem repositorySystem;

    @Override
    public Collection<?> resolve(String groupId, String artifactId, String version, String packaging, String classifier,
            String dependencyScope, int depth, Collection<MavenArtifactRepositoryReference> additionalRepositories,
            Object session) throws DependencyResolutionException {
        Artifact artifact;
        if (classifier != null && !classifier.isEmpty()) {
            artifact = repositorySystem.createArtifactWithClassifier(groupId, artifactId, version, packaging,
                    classifier);
        } else {
            artifact = repositorySystem.createArtifact(groupId, artifactId, version, null, packaging);
        }
        logger.debug("Resolve " + artifact + "...");
        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifact);
        MavenSession mavenSession = getMavenSession(session);
        request.setResolveRoot(true);
        request.setOffline(mavenSession.isOffline());
        request.setResolutionFilter(new ArtifactFilter() {

            @Override
            public boolean include(Artifact a) {
                List<String> trail = a.getDependencyTrail();
                if (logger.isDebugEnabled()) {
                    logger.debug("[depth=" + trail.size() + ", scope matches =" + isValidScope(a, dependencyScope)
                            + "][" + a + "][" + trail.stream().collect(Collectors.joining(" >> ")) + "]");
                }
                if (trail.size() <= depth) {
                    return isValidScope(a, dependencyScope);
                }
                return false;
            }
        });
        request.setLocalRepository(mavenSession.getLocalRepository());
        request.setResolveTransitively(depth > 0);
        if (additionalRepositories != null && additionalRepositories.size() > 0) {
            List<ArtifactRepository> repositories = new ArrayList<>(
                    mavenSession.getCurrentProject().getRemoteArtifactRepositories());
            for (MavenArtifactRepositoryReference reference : additionalRepositories) {
                repositories.add(repositorySystem.createArtifactRepository(reference.getId(), reference.getUrl(), null,
                        null, null));
            }
            request.setRemoteRepositories(repositorySystem.getEffectiveRepositories(repositories));
        } else {
            request.setRemoteRepositories(mavenSession.getCurrentProject().getRemoteArtifactRepositories());
        }
        ArtifactResolutionResult result = repositorySystem.resolve(request);
        if (result.hasExceptions()) {
            throw new DependencyResolutionException("resolving " + artifact + " failed!", result.getExceptions());
        }
        return result.getArtifacts().stream().filter(a -> a.getFile() != null).map(MavenArtifactFacade::new)
                .collect(Collectors.toList());
    }

    protected boolean isValidScope(Artifact artifact, String desiredScope) {
        String artifactScope = artifact.getScope();
        if (artifactScope == null || artifactScope.isBlank()) {
            return true;
        }
        //compile is the default scope if not specified see
        // https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#dependency-scope
        if (desiredScope == null || desiredScope.isBlank() || SCOPE_COMPILE.equalsIgnoreCase(desiredScope)) {
            return SCOPE_COMPILE.equalsIgnoreCase(artifactScope);
        }
        if (SCOPE_PROVIDED.equalsIgnoreCase(desiredScope)) {
            return SCOPE_PROVIDED.equalsIgnoreCase(artifactScope) || SCOPE_COMPILE.equalsIgnoreCase(artifactScope)
                    || SCOPE_SYSTEM.equalsIgnoreCase(artifactScope) || SCOPE_RUNTIME.equalsIgnoreCase(artifactScope);
        }
        if (SCOPE_TEST.equalsIgnoreCase(desiredScope)) {
            return SCOPE_TEST.equalsIgnoreCase(artifactScope) || SCOPE_COMPILE.equalsIgnoreCase(artifactScope)
                    || SCOPE_PROVIDED.equalsIgnoreCase(artifactScope) || SCOPE_SYSTEM.equalsIgnoreCase(artifactScope)
                    || SCOPE_RUNTIME.equalsIgnoreCase(artifactScope);
        }
        //invalid scope type
        return false;
    }

    protected MavenSession getMavenSession(Object session) {
        MavenSession mavenSession;
        if (session instanceof MavenSession) {
            mavenSession = (MavenSession) session;
        } else {
            mavenSession = Objects.requireNonNull(context.getSession(),
                    "Can't acquire maven session from context, called outside maven thread context?");
        }
        return mavenSession;
    }

    @Override
    public void afterFrameworkStarted(EmbeddedEquinox framework) {
        framework.registerService(MavenDependenciesResolver.class, this);
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
                return model.getGroupId();
            }

            @Override
            public String getArtifactId() {
                return model.getArtifactId();
            }

            @Override
            public String getVersion() {
                return model.getVersion();
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
