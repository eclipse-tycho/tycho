/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.runtime;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.sisu.equinox.embedder.EquinoxRuntimeLocator.EquinoxRuntimeDescription;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.core.osgitools.DefaultArtifactKey;
import org.eclipse.tycho.core.osgitools.targetplatform.BasicDependencyArtifacts;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.model.ProductConfiguration;

class WorkspaceDependencyResolver {
    private static final String SUFFIX_BASEDIR = ":basedir";

    private static final String SUFFIX_LOCATION = ":location";

    private static final String SUFFIX_ENTRIES = ":entries";

    private static final String SYSPROP_STATELOCATION = "m2etycho.workspace.state";

    private static final String FILE_WORKSPACESTATE = "workspacestate.properties";

    private final DependencyResolver mavenResolver;

    private final File stateLocation;

    /**
     * All workspace project and target platform bundles, as reported by PDE. Workspace projects are
     * expected to shadow target platform bundles with the same Bundle-SymbolicName.
     */
    private final BasicDependencyArtifacts workspaceBundles;

    /**
     * Maps bundle location to dev.properties entries of the bundle
     */
    private final Map<File, String> workspaceDeventries;

    /**
     * Maps workspace project basedir to corresponding bundle location
     */
    private final Map<File, File> workspaceBasedirs;

    /**
     * dev.properties entries of bundles resolved by this resolver instance.
     */
    private Properties deventries = new Properties();

    private WorkspaceDependencyResolver(DependencyResolver mavenResolver, String stateLocation) {
        Map<File, String> workspaceDeventries = new HashMap<File, String>();
        Map<File, File> workspaceBasedirs = new HashMap<File, File>();
        BasicDependencyArtifacts workspaceBundles = new BasicDependencyArtifacts();
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
        this.mavenResolver = mavenResolver;
        this.workspaceBundles = workspaceBundles;
        this.workspaceBasedirs = Collections.unmodifiableMap(workspaceBasedirs);
        this.workspaceDeventries = Collections.unmodifiableMap(workspaceDeventries);
    }

    private File toLocation(String location) {
        return new File(location);
    }

    private DefaultArtifactKey toArtifactKey(String key) {
        StringTokenizer st = new StringTokenizer(key, ":");
        String type = st.nextToken();
        String id = st.nextToken();
        String version = st.nextToken();
        return new DefaultArtifactKey(type, id, version);
    }

    private Properties loadWorkspaceState(File workspaceState) {
        Properties properties = new Properties();
        try {
            InputStream is = new BufferedInputStream(new FileInputStream(workspaceState));
            try {
                properties.load(is);
            } finally {
                IOUtil.close(is);
            }
        } catch (IOException e) {
            // silently ignore for now
        }
        return properties;
    }

    private void addProduct(EquinoxRuntimeDescription result, Artifact pom) throws IOException, MavenExecutionException {
        ProductConfiguration product = ProductConfiguration.read(new File(pom.getFile().getParentFile(), pom
                .getArtifactId() + ".product"));

        // the above fails with IOException if .product file is not available or can't be read
        // we get here only when we have valid product instance

        Set<String> missing = new LinkedHashSet<String>();
        for (PluginRef pluginRef : product.getPlugins()) {
            ArtifactDescriptor artifact = workspaceBundles.getArtifact(ArtifactKey.TYPE_ECLIPSE_PLUGIN,
                    pluginRef.getId(), pluginRef.getVersion());
            if (artifact != null) {
                if (!"org.eclipse.osgi".equals(artifact.getKey().getId())) {
                    addBundle(result, artifact);
                }
            } else {
                missing.add(pluginRef.toString());
            }
        }

        if (!missing.isEmpty()) {
            throw new MavenExecutionException("Inconsistent m2e-tycho workspace state, missing bundles: "
                    + missing.toString(), (Throwable) null);
        }
    }

    private String toStringKey(ArtifactKey key) {
        StringBuilder sb = new StringBuilder();
        sb.append(key.getType()); // TODO eclipse-test-plugin => eclipse-plugin
        sb.append(':').append(key.getId()).append(':').append(key.getVersion());
        return sb.toString();
    }

    public static WorkspaceDependencyResolver getResolver(DependencyResolver mavenResolver) {
        String stateLocation = System.getProperty(SYSPROP_STATELOCATION);
        if (stateLocation == null) {
            return null;
        }

        return new WorkspaceDependencyResolver(mavenResolver, stateLocation);
    }

    public boolean addRuntimeArtifact(EquinoxRuntimeDescription description, MavenSession session, Dependency dependency)
            throws MavenExecutionException {
        Dependency dependencyPom = new Dependency();
        dependencyPom.setType("pom");
        dependencyPom.setGroupId(dependency.getGroupId());
        dependencyPom.setArtifactId(dependency.getArtifactId());
        dependencyPom.setVersion(dependency.getVersion());
        Artifact pom;
        try {
            pom = mavenResolver.resolveDependency(session, dependencyPom);
        } catch (MavenExecutionException e) {
            // not our problem, let default implementation deal with it
            return false;
        }
        boolean result;
        try {
            if ("zip".equals(dependency.getType())) {
                addProduct(description, pom);
            } else {
                addBundle(description, pom);
            }
            result = true;
        } catch (IOException e) {
            // TODO log or throw an exception
            result = false;
        }
        return result;
    }

    private void addBundle(EquinoxRuntimeDescription result, Artifact pom) {
        File location = workspaceBasedirs.get(pom.getFile().getParentFile());
        if (location != null) {
            ArtifactDescriptor descriptor = workspaceBundles.getArtifact(location).get(null); // main artifact only
            addBundle(result, descriptor);
        }
    }

    protected void addBundle(EquinoxRuntimeDescription result, ArtifactDescriptor bundle) {
        result.addBundle(bundle.getLocation());
        String deventries = workspaceDeventries.get(bundle.getLocation());
        if (deventries != null) {
            this.deventries.put(bundle.getKey().getId(), deventries);
        }
    }

    public void addPlatformProperties(EquinoxRuntimeDescription result) throws MavenExecutionException {
        result.addPlatformProperty("osgi.install.area", stateLocation.getAbsolutePath());

        File devproperties = new File(stateLocation, "dev.properties");
        try {
            OutputStream os = new BufferedOutputStream(new FileOutputStream(devproperties));
            try {
                deventries.store(os, null);
            } finally {
                IOUtil.close(os);
            }
            result.addPlatformProperty("osgi.dev", devproperties.toURI().toURL().toExternalForm());
        } catch (IOException e) {
            throw new MavenExecutionException("Could not write dev.properties", e);
        }
    }
}
