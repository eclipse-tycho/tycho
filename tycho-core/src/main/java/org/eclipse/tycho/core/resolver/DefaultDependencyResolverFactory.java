/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - #486 - Remove LocalDependencyResolver 
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import java.util.Objects;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.DependencyResolver;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.utils.TychoProjectUtils;

@Component(role = DefaultDependencyResolverFactory.class)
public class DefaultDependencyResolverFactory {
    public static final String DEFAULT_RESOLVER_HINT = "p2";

    @Requirement
    private Logger logger;

    @Requirement
    private PlexusContainer container;

    public DependencyResolver lookupDependencyResolver(MavenProject project) {
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        TargetPlatformConfiguration configuration = TychoProjectUtils.getTargetPlatformConfiguration(reactorProject);
        String resolverRole = Objects.requireNonNullElse(configuration.getTargetPlatformResolver(),
                DEFAULT_RESOLVER_HINT);
        try {
            return container.lookup(DependencyResolver.class, resolverRole);
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Could not instantiate required component DependencyResolver with role " + resolverRole, e);
        }
    }
}
