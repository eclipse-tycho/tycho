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
 *******************************************************************************/
package org.eclipse.tycho.helper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.LifecyclePluginResolver;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.PluginDescriptorCache;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.ComponentDependency;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;

/**
 * Helper class that allows execution of components from maven plugin class realms. Normally, these
 * components are not visible from tycho-core extensions plugin and require treatment. Typical usage
 * 
 * <pre>
 * &#64;Requirement
 * private EquinoxServiceFactory equinox;
 * 
 * &#64;Requirement
 * private PluginRealmHelper pluginRealmHelper;
 * 
 * ...
 * 
 * public void someMethod(final MavenSession session, final MavenProject project) throws MavenExecutionException {
 *    pluginRealmHelper..execute(session, project, new Runnable() {
 *        public void run() {
 *            try {
 *                equinox.lookup(SomeComponent.class).someComponentMethod();
 *            } catch (ComponentLookupException e) {
 *                // have not found anything
 *            }
 *        }
 *    }, new PluginFilter() {
 *        public boolean accept(PluginDescriptor descriptor) {
 *            return true if the plugin is relevant;
 *        }
 *    });
 * }
 * </pre>
 * 
 */
@Named
public class DefaultPluginRealmHelper implements PluginRealmHelper {
    public static interface PluginFilter {
        public boolean accept(PluginDescriptor descriptor);
    }

    @Inject
    private Logger logger;

    @Inject
    private MavenPluginManager pluginManager;

    @Inject
    private BuildPluginManager buildPluginManager;

    @Inject
    private PluginDescriptorCache pluginDescriptorCache;

    @Inject
    private LifecyclePluginResolver lifecyclePluginResolver;

    @Inject
    protected MavenPluginManager mavenPluginManager;

    @Inject
    private PlexusContainer plexus;

    @Inject
    private ProjectHelper projectHelper;

    public <T> void visitPluginExtensions(MavenProject project, MavenSession mavenSession, Class<T> type,
            Consumer<? super T> consumer) throws PluginVersionResolutionException, PluginDescriptorParsingException,
            InvalidPluginDescriptorException, PluginResolutionException, PluginManagerException {
        Set<String> visited = new HashSet<String>();
        execute(project, mavenSession, () -> {
            try {
                plexus.lookupList(type).stream().filter(x -> visited.add(x.getClass().getName())).forEach(consumer);
            } catch (ComponentLookupException e) {
                logger.debug("Cannot lookup any item of type: " + type);
            }
        }, DefaultPluginRealmHelper::isTychoEmbedderPlugin);
    }

    public void execute(MavenProject project, MavenSession mavenSession, Runnable runnable, PluginFilter filter)
            throws PluginVersionResolutionException, PluginDescriptorParsingException, InvalidPluginDescriptorException,
            PluginResolutionException, PluginManagerException {
        if (mavenSession.getLocalRepository() == null) {
            // This happens in some test-code ... but should never happen in real maven...
            return;
        }
        MavenSession executeSession = mavenSession.clone();
        executeSession.setCurrentProject(project);
        lifecyclePluginResolver.resolveMissingPluginVersions(project, executeSession);
        List<Plugin> plugins = projectHelper.getPlugins(project, executeSession);
        for (Plugin plugin : plugins) {
            if (plugin.isExtensions()) {
                // due to maven classloading model limitations, build extensions plugins cannot
                // share classes
                // since tycho core, i.e. this code, is loaded as a build extension, no other
                // extensions plugin
                // can load classes from tycho core
                // https://cwiki.apache.org/MAVEN/maven-3x-class-loading.html
                continue;
            }
            PluginDescriptor pluginDescriptor;
            try {
                pluginDescriptor = mavenPluginManager.getPluginDescriptor(plugin, project.getRemotePluginRepositories(),
                        executeSession.getRepositorySession());
                // session);
            } catch (PluginResolutionException e) {
                // if the plugin really does not exist, the Maven build will fail later on
                // anyway -> ignore for now (cf. bug #432957)
                logger.debug("PluginResolutionException while looking for components from " + plugin, e);
                continue;
            }
            if (pluginDescriptor != null) {
                if (filter == null || filter.accept(pluginDescriptor)) {
                    ClassRealm pluginRealm = buildPluginManager.getPluginRealm(executeSession, pluginDescriptor);
                    if (pluginRealm != null) {
                        ClassLoader origTCCL = Thread.currentThread().getContextClassLoader();
                        try {
                            Thread.currentThread().setContextClassLoader(pluginRealm);
                            runnable.run();
                        } finally {
                            Thread.currentThread().setContextClassLoader(origTCCL);
                        }
                    }
                }
            }
        }

    }

    private static boolean isTychoEmbedderPlugin(PluginDescriptor pluginDescriptor) {
        if (pluginDescriptor.getArtifactMap().containsKey("org.eclipse.tycho:tycho-spi")) {
            return true;
        }
        for (ComponentDependency dependency : pluginDescriptor.getDependencies()) {
            if ("org.eclipse.tycho".equals(dependency.getGroupId()) && "tycho-spi".equals(dependency.getArtifactId())) {
                return true;
            }
        }
        return false;
    }
}
