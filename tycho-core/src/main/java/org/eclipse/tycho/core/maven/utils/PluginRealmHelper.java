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
package org.eclipse.tycho.core.maven.utils;

import org.apache.maven.MavenExecutionException;
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
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * Helper class that allows execution of components from maven plugin class realms. Normally, these
 * components are not visible from tycho-core extensions plugin and require treatment. Typical usage
 * 
 * <pre>
 * @Requirement
 * private EquinoxServiceFactory equinox;
 * 
 * @Requirement
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
@Component(role = PluginRealmHelper.class)
public class PluginRealmHelper {
    public static interface PluginFilter {
        public boolean accept(PluginDescriptor descriptor);
    };

    @Requirement
    private MavenPluginManager pluginManager;

    @Requirement
    private BuildPluginManager buildPluginManager;

    @Requirement
    private PluginDescriptorCache pluginDescriptorCache;

    @Requirement
    private LifecyclePluginResolver lifecyclePluginResolver;

    public void execute(MavenSession session, MavenProject project, Runnable runnable, PluginFilter filter)
            throws MavenExecutionException {
        for (Plugin plugin : project.getBuildPlugins()) {
            if (plugin.isExtensions()) {
                // due to maven classloading model limitations, build extensions plugins cannot share classes
                // since tycho core, i.e. this code, is loaded as a build extension, no other extensions plugin
                // can load classes from tycho core
                // https://cwiki.apache.org/MAVEN/maven-3x-class-loading.html
                continue;
            }
            try {
                lifecyclePluginResolver.resolveMissingPluginVersions(project, session);
                PluginDescriptor pluginDescriptor = pluginManager.getPluginDescriptor(plugin,
                        project.getRemotePluginRepositories(), session.getRepositorySession());

                if (pluginDescriptor != null) {
                    if (pluginDescriptor.getArtifactMap().isEmpty() && pluginDescriptor.getDependencies().isEmpty()) {
                        // force plugin descriptor reload to workaround http://jira.codehaus.org/browse/MNG-5212
                        // this branch won't be executed on 3.0.5+, where MNG-5212 is fixed already
                        PluginDescriptorCache.Key descriptorCacheKey = pluginDescriptorCache.createKey(plugin,
                                project.getRemotePluginRepositories(), session.getRepositorySession());
                        pluginDescriptorCache.put(descriptorCacheKey, null);
                        pluginDescriptor = pluginManager.getPluginDescriptor(plugin,
                                project.getRemotePluginRepositories(), session.getRepositorySession());
                    }

                    if (filter == null || filter.accept(pluginDescriptor)) {
                        ClassRealm pluginRealm;
                        MavenProject oldCurrentProject = session.getCurrentProject();
                        session.setCurrentProject(project);
                        try {
                            pluginRealm = buildPluginManager.getPluginRealm(session, pluginDescriptor);
                        } finally {
                            session.setCurrentProject(oldCurrentProject);
                        }
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
            } catch (PluginManagerException e) {
                throw newMavenExecutionException(e);
            } catch (PluginResolutionException e) {
                throw newMavenExecutionException(e);
            } catch (PluginVersionResolutionException e) {
                throw newMavenExecutionException(e);
            } catch (PluginDescriptorParsingException e) {
                throw newMavenExecutionException(e);
            } catch (InvalidPluginDescriptorException e) {
                throw newMavenExecutionException(e);
            }
        }

    }

    private static MavenExecutionException newMavenExecutionException(Exception cause) {
        return new MavenExecutionException("Could not setup plugin ClassRealm", cause);
    }
}
