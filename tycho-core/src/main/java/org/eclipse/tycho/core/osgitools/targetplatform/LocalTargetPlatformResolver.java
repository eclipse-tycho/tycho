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
package org.eclipse.tycho.core.osgitools.targetplatform;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import org.apache.maven.ProjectDependenciesResolver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.TargetPlatform;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TargetPlatformResolver;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DefaultArtifactKey;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.model.Feature;
import org.osgi.framework.Constants;

/**
 * Creates target platform based on local eclipse installation.
 */
@Component(role = TargetPlatformResolver.class, hint = LocalTargetPlatformResolver.ROLE_HINT, instantiationStrategy = "per-lookup")
public class LocalTargetPlatformResolver extends AbstractTargetPlatformResolver implements TargetPlatformResolver {

    public static final String ROLE_HINT = "local";

    @Requirement
    private EclipseInstallationLayout layout;

    @Requirement
    private BundleReader manifestReader;

    @Requirement
    private ProjectDependenciesResolver projectDependenciesResolver;

    @Requirement(role = TychoProject.class)
    private Map<String, TychoProject> projectTypes;

    private boolean isSubdir(File parent, File child) {
        return child.getAbsolutePath().startsWith(parent.getAbsolutePath());
    }

    private void addProjects(MavenSession session, DefaultTargetPlatform platform) {
        File parentDir = null;

        for (MavenProject project : session.getProjects()) {
            ReactorProject projectProxy = DefaultReactorProject.adapt(project);
            TychoProject dr = projectTypes.get(project.getPackaging());
            if (dr != null) {
                ArtifactKey key = dr.getArtifactKey(projectProxy);

                platform.removeAll(key.getType(), key.getId());

                platform.addReactorArtifact(key, projectProxy, null, null);

                if (parentDir == null || isSubdir(project.getBasedir(), parentDir)) {
                    parentDir = project.getBasedir();
                }
            }
        }

        platform.addSite(parentDir);
    }

    public TargetPlatform resolvePlatform(MavenSession session, MavenProject project,
            List<ReactorProject> reactorProjects, List<Dependency> dependencies) {
        DefaultTargetPlatform platform = new DefaultTargetPlatform();

        for (File site : layout.getSites()) {
            platform.addSite(site);

            for (File plugin : layout.getPlugins(site)) {
                ArtifactKey artifactKey = getArtifactKey(session, plugin);

                if (artifactKey != null) {
                    platform.addArtifactFile(artifactKey, plugin, null);
                }
            }

            for (File feature : layout.getFeatures(site)) {
                Feature desc = Feature.loadFeature(feature);
                ArtifactKey key = new DefaultArtifactKey(org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_FEATURE,
                        desc.getId(), desc.getVersion());

                platform.addArtifactFile(key, feature, null);
            }
        }

        addProjects(session, platform);
        addDependencies(session, project, platform);

        if (platform.isEmpty()) {
            getLogger().warn("Could not find any bundles or features in " + layout.getLocation());
        }

        return platform;
    }

    private void addDependencies(MavenSession session, MavenProject project, DefaultTargetPlatform platform) {
        TargetPlatformConfiguration configuration = (TargetPlatformConfiguration) project
                .getContextValue(TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION);

        if (configuration != null
                && TargetPlatformConfiguration.POM_DEPENDENCIES_CONSIDER.equals(configuration.getPomDependencies())) {
            Map<String, MavenProject> projectIds = new HashMap<String, MavenProject>(session.getProjects().size() * 2);
            // make a list of reactor projects
            for (MavenProject p : session.getProjects()) {
                String key = ArtifactUtils.key(p.getGroupId(), p.getArtifactId(), p.getVersion());
                projectIds.put(key, p);
            }
            // handle dependencies that are in reactor
            for (Dependency dependency : project.getDependencies()) {
                if (Artifact.SCOPE_COMPILE.equals(dependency.getScope())) {
                    String key = ArtifactUtils.key(dependency.getGroupId(), dependency.getArtifactId(),
                            dependency.getVersion());
                    if (projectIds.containsKey(key)) {
                        MavenProject dependent = projectIds.get(key);
                        ArtifactKey artifactKey = getArtifactKey(session, dependent);
                        if (artifactKey != null) {
                            platform.removeAll(artifactKey.getType(), artifactKey.getId());
                            ReactorProject projectProxy = DefaultReactorProject.adapt(dependent);
                            platform.addReactorArtifact(artifactKey, projectProxy, null, null);
                            if (getLogger().isDebugEnabled()) {
                                getLogger().debug("Add Maven project " + artifactKey);
                            }
                        }
                    }
                }
            }
            // handle rest of dependencies
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
                    if (projectIds.containsKey(key)) {
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
            for (Artifact artifact : artifacts) {
                String key = ArtifactUtils.key(artifact.getGroupId(), artifact.getArtifactId(),
                        artifact.getBaseVersion());
                if (!projectIds.containsKey(key)) {
                    File plugin = artifact.getFile();
                    ArtifactKey artifactKey = getArtifactKey(session, plugin);

                    if (artifactKey != null) {
                        platform.addArtifactFile(artifactKey, plugin, null);
                        if (getLogger().isDebugEnabled()) {
                            getLogger().debug("Add Maven artifact " + artifactKey);
                        }
                    }
                }
            }
        }
    }

    public ArtifactKey getArtifactKey(MavenSession session, MavenProject project) {
        Manifest mf = manifestReader.loadManifest(project.getBasedir());

        if (mf == null) {
            return null;
        }

        ManifestElement[] id = manifestReader.parseHeader(Constants.BUNDLE_SYMBOLICNAME, mf);
        ManifestElement[] version = manifestReader.parseHeader(Constants.BUNDLE_VERSION, mf);

        if (id == null || version == null) {
            return null;
        }

        ArtifactKey key = new DefaultArtifactKey(org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_PLUGIN, id[0].getValue(),
                version[0].getValue());
        return key;
    }

    public ArtifactKey getArtifactKey(MavenSession session, File plugin) {
        Manifest mf = manifestReader.loadManifest(plugin);

        if (mf == null) {
            return null;
        }

        ManifestElement[] id = manifestReader.parseHeader(Constants.BUNDLE_SYMBOLICNAME, mf);
        ManifestElement[] version = manifestReader.parseHeader(Constants.BUNDLE_VERSION, mf);

        if (id == null || version == null) {
            return null;
        }

        ArtifactKey key = new DefaultArtifactKey(org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_PLUGIN, id[0].getValue(),
                version[0].getValue());
        return key;
    }

    public void setLocation(File location) throws IOException {
        layout.setLocation(location.getCanonicalFile());
    }

    public void setupProjects(MavenSession session, MavenProject project, ReactorProject reactorProject) {
        // TODO Auto-generated method stub

    }
}
