/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.core.maven.utils;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.PluginDescriptorCache;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Facade to hide aether incompatiblities introduced with maven 3.1
 */
public interface CompatibilityFacade {

    public PluginDescriptor getPluginDescriptor(Plugin plugin, List<RemoteRepository> repositories, MavenSession session)
            throws PluginResolutionException, PluginDescriptorParsingException, InvalidPluginDescriptorException;

    public PluginDescriptorCache.Key createKey(Plugin plugin, List<RemoteRepository> repositories, MavenSession session)
            throws PluginResolutionException, PluginDescriptorParsingException, InvalidPluginDescriptorException;

}
