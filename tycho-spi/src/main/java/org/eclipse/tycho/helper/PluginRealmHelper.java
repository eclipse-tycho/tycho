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

import java.util.function.Consumer;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.helper.DefaultPluginRealmHelper.PluginFilter;

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
public interface PluginRealmHelper {

    <T> void visitPluginExtensions(MavenProject project, MavenSession mavenSession, Class<T> type,
            Consumer<? super T> consumer) throws PluginVersionResolutionException, PluginDescriptorParsingException,
            InvalidPluginDescriptorException, PluginResolutionException, PluginManagerException;

    void execute(MavenProject project, MavenSession mavenSession, Runnable runnable, PluginFilter filter)
            throws PluginVersionResolutionException, PluginDescriptorParsingException, InvalidPluginDescriptorException,
            PluginResolutionException, PluginManagerException;

}
