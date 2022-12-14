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
 *    Christoph Läubrich - #486 - Remove org.eclipse.tycho.core.osgitools.targetplatform.LocalDependencyResolver  
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import java.util.Properties;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.DependencyResolver;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;

@Component(role = DefaultDependencyResolverFactory.class)
public class DefaultDependencyResolverFactory {
    public static final String DEFAULT_RESOLVER_HINT = "p2";

    @Requirement
    private Logger logger;

    @Requirement
    private PlexusContainer container;
    @Requirement(hint = DEFAULT_RESOLVER_HINT)
    private DependencyResolver dependencyResolver;

    public DependencyResolver lookupDependencyResolver(MavenProject project) {
        return lookupDependencyResolver(DefaultReactorProject.adapt(project));
    }

    public DependencyResolver lookupDependencyResolver(ReactorProject reactorProject) {

        Properties properties = reactorProject.getProperties();
        if (properties.getProperty("tycho.targetPlatform") != null) {
            throw new RuntimeException("-Dtycho.targetPlatform was deprecated and IS REMOVED in this tycho version.");
        }
        if (properties.getProperty("tycho.test.targetPlatform") != null) {
            throw new RuntimeException("don't use that: " + properties.getProperty("tycho.test.targetPlatform"));
        }
        return dependencyResolver;
    }
}
