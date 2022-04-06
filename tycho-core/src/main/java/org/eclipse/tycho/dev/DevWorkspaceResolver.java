/*******************************************************************************
 * Copyright (c) 2012, 2020 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.dev;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.core.osgitools.targetplatform.ArtifactCollection;

@Component(role = DevWorkspaceResolver.class)
public class DevWorkspaceResolver implements Initializable {
    private static final String SUFFIX_BASEDIR = ":basedir";

    private static final String SUFFIX_LOCATION = ":location";

    private static final String SUFFIX_ENTRIES = ":entries";

    /**
     * Location of m2e.tycho workspace state location.
     * <p/>
     * Value must match among tycho-insitu, DevelopmentWorkspaceState and
     * AbstractTychoIntegrationTest.
     */
    private static final String SYSPROP_STATELOCATION = "tychodev.workspace.state";

    private static final String FILE_WORKSPACESTATE = "workspacestate.properties";

    @Requirement
    private RepositorySystem repositorySystem;

    private File stateLocation;

    /**
     * All workspace project and target platform bundles, as reported by PDE. Workspace projects are
     * expected to shadow target platform bundles with the same Bundle-SymbolicName.
     */
    private ArtifactCollection workspaceBundles;

    /**
     * Maps bundle location to dev.properties entries of the bundle
     */
    private Map<File, String> workspaceDeventries;

    /**
     * Maps workspace project basedir to corresponding bundle location
     */
    private Map<File, File> workspaceBasedirs;

    @Override
    public void initialize() throws InitializationException {
        Map<File, String> workspaceDeventries = new HashMap<>();
        Map<File, File> workspaceBasedirs = new HashMap<>();
        ArtifactCollection workspaceBundles = new ArtifactCollection();

        String stateLocation = System.getProperty(SYSPROP_STATELOCATION);
        if (stateLocation != null) {
            Properties properties = loadWorkspaceState(new File(stateLocation, FILE_WORKSPACESTATE));
            for (Object key : properties.keySet()) {
                String stringKey = (String) key;
                if (stringKey.endsWith(SUFFIX_LOCATION)) {
                    DefaultArtifactKey artifactKey = toArtifactKey(stringKey);
                    File location = toLocation(properties.getProperty(stringKey));
                    workspaceBundles.addArtifactFile(artifactKey, location, null);

                    // workspace projects
                    stringKey = toStringKey(artifactKey); // normalize
                    String basedir = properties.getProperty(stringKey + SUFFIX_BASEDIR);
                    if (basedir != null) {
                        workspaceBasedirs.put(new File(basedir), location);
                        String deventries = properties.getProperty(stringKey + SUFFIX_ENTRIES);
                        if (deventries != null) {
                            workspaceDeventries.put(location, deventries);
                        }
                    }
                }
            }
            this.stateLocation = new File(stateLocation);
        }

        this.workspaceBundles = workspaceBundles;
        this.workspaceBasedirs = Collections.unmodifiableMap(workspaceBasedirs);
        this.workspaceDeventries = Collections.unmodifiableMap(workspaceDeventries);
    }

    private Properties loadWorkspaceState(File workspaceState) {
        Properties properties = new Properties();
        try (InputStream is = new BufferedInputStream(new FileInputStream(workspaceState))) {
            properties.load(is);
        } catch (IOException e) {
            // silently ignore for now
        }
        return properties;
    }

    private DefaultArtifactKey toArtifactKey(String key) {
        StringTokenizer st = new StringTokenizer(key, ":");
        String type = st.nextToken();
        String id = st.nextToken();
        String version = st.nextToken();
        return new DefaultArtifactKey(type, id, version);
    }

    private File toLocation(String location) {
        return new File(location);
    }

    private String toStringKey(ArtifactKey key) {
        StringBuilder sb = new StringBuilder();
        sb.append(key.getType()); // TODO eclipse-test-plugin => eclipse-plugin
        sb.append(':').append(key.getId()).append(':').append(key.getVersion());
        return sb.toString();
    }

    public DevBundleInfo getBundleInfo(String symbolicName, String version) {
        ArtifactDescriptor descriptor = workspaceBundles.getArtifact(ArtifactType.TYPE_ECLIPSE_PLUGIN, symbolicName,
                version);
        return newBundleInfo(descriptor);
    }

    private DevBundleInfo newBundleInfo(ArtifactDescriptor descriptor) {
        if (descriptor == null) {
            return null;
        }
        File location = descriptor.getLocation(true);
        return new DevBundleInfo(descriptor.getKey(), location, workspaceDeventries.get(location));
    }

    public DevBundleInfo getBundleInfo(File projectBasedir) {
        File location = workspaceBasedirs.get(projectBasedir);

        if (location != null) {
            ArtifactDescriptor descriptor = workspaceBundles.getArtifact(location).get(null); // main artifact only
            return newBundleInfo(descriptor);
        }

        return null;
    }

    public DevBundleInfo getBundleInfo(MavenSession session, String groupId, String artifacyId, String version,
            List<ArtifactRepository> repositories) {

        Artifact pomArtifact = repositorySystem.createArtifact(groupId, artifacyId, version, "pom");

        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(pomArtifact);
        request.setResolveRoot(true).setResolveTransitively(false);
        request.setLocalRepository(session.getLocalRepository());
        request.setRemoteRepositories(repositories);
        request.setCache(session.getRepositoryCache());
        request.setOffline(session.isOffline());
        request.setForceUpdate(session.getRequest().isUpdateSnapshots());

        ArtifactResolutionResult result = repositorySystem.resolve(request);

        if (result.isSuccess()) {
            return getBundleInfo(pomArtifact.getFile().getParentFile());
        }

        return null;
    }

    public File getStateLocation() {
        return stateLocation;
    }
}
