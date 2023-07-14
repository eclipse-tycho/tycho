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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
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
@Component(role = PluginRealmHelper.class)
public class PluginRealmHelper {
    public static interface PluginFilter {
        public boolean accept(PluginDescriptor descriptor);
    }

    @Requirement
    private Logger logger;

    @Requirement
    private MavenPluginManager pluginManager;

    @Requirement
    private BuildPluginManager buildPluginManager;

    @Requirement
    private PluginDescriptorCache pluginDescriptorCache;

    @Requirement
    private LifecyclePluginResolver lifecyclePluginResolver;

    @Requirement
    protected MavenPluginManager mavenPluginManager;

    @Requirement
    private PlexusContainer plexus;

    @Requirement
    private ProjectHelper projectHelper;

    public <T> void visitPluginExtensions(MavenProject project, MavenSession mavenSession, Class<T> type,
            Consumer<? super T> consumer) throws PluginVersionResolutionException, PluginDescriptorParsingException,
            InvalidPluginDescriptorException, PluginResolutionException, PluginManagerException {
        visitPluginExtensions(project, mavenSession, type, x -> {
            consumer.accept(x);
            return true;
        });
    }

    public <T> void visitPluginExtensions(MavenProject project, MavenSession mavenSession, Class<T> type,
            Predicate<? super T> consumer) throws PluginVersionResolutionException, PluginDescriptorParsingException,
            InvalidPluginDescriptorException, PluginResolutionException, PluginManagerException {
        visitPluginExtensions(project, mavenSession, type, null, consumer);
    }

    public <T> void visitPluginExtensions(MavenProject project, MavenSession mavenSession, Class<T> type, String hint,
            Predicate<? super T> consumer) throws PluginVersionResolutionException, PluginDescriptorParsingException,
            InvalidPluginDescriptorException, PluginResolutionException, PluginManagerException {
        visit(project, mavenSession, type, hint, consumer, false);
    }

    public <T> void visitExtensions(MavenProject project, MavenSession mavenSession, Class<T> type, String hint,
            Predicate<? super T> consumer) throws PluginVersionResolutionException, PluginDescriptorParsingException,
            InvalidPluginDescriptorException, PluginResolutionException, PluginManagerException {
        visit(project, mavenSession, type, hint, consumer, true);
    }

    private <T> void visit(MavenProject project, MavenSession mavenSession, Class<T> type, String hint,
            Predicate<? super T> consumer, boolean self)
            throws PluginVersionResolutionException, PluginDescriptorParsingException, InvalidPluginDescriptorException,
            PluginResolutionException, PluginManagerException {
        Set<String> visited = new HashSet<String>();
        BooleanSupplier supplier = () -> {
            try {
                if (hint == null) {
                    Iterator<T> iterator = plexus.lookupList(type).stream()
                            .filter(x -> visited.add(x.getClass().getName())).iterator();
                    while (iterator.hasNext()) {
                        T component = (T) iterator.next();
                        if (!consumer.test(component)) {
                            return false;
                        }
                    }
                } else {
                    Map<String, T> map = plexus.lookupMap(type);
                    T component = map.get(hint);
                    if (component != null && visited.add(component.getClass().getName())) {
                        return consumer.test(component);
                    }
                }
            } catch (ComponentLookupException e) {
                logger.debug("Cannot lookup any item of type: " + type);
            }
            return true;
        };
        if (!self || supplier.getAsBoolean()) { // this executes in the caller realm
            //this then executes in the plugin realms...
            execute(project, mavenSession, supplier, PluginRealmHelper::isTychoEmbedderPlugin);
        }
    }

    private void execute(MavenProject project, MavenSession mavenSession, BooleanSupplier runnable, PluginFilter filter)
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
                            if (!runnable.getAsBoolean()) {
                                return;
                            }
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
