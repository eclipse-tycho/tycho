/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - #486 - Remove org.eclipse.tycho.core.osgitools.targetplatform.LocalDependencyResolver  
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import java.util.Objects;
import java.util.Properties;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TychoConstants;
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
        Properties properties = (Properties) reactorProject.getContextValue(TychoConstants.CTX_MERGED_PROPERTIES);
        TargetPlatformConfiguration configuration = TychoProjectUtils.getTargetPlatformConfiguration(reactorProject);

        if (properties.getProperty("tycho.targetPlatform") != null) {
            throw new RuntimeException("-Dtycho.targetPlatform was deprecated and IS REMOVED in this tycho version.");
        }
        if (properties.getProperty("tycho.test.targetPlatform") != null) {
            try {
                @SuppressWarnings("deprecation")
                String hint = org.eclipse.tycho.core.osgitools.targetplatform.LocalDependencyResolver.ROLE_HINT;
                return container.lookup(DependencyResolver.class, hint);
            } catch (ComponentLookupException e) {
                throw new RuntimeException("Could not instantiate required component", e);
            }
        }
        String resolverRole = Objects.requireNonNullElse(configuration.getTargetPlatformResolver(),
                DEFAULT_RESOLVER_HINT);
        try {
            return container.lookup(DependencyResolver.class, resolverRole);
        } catch (ComponentLookupException e) {
            throw new RuntimeException(
                    "Could not instantiate requested DependencyResolver component with role = " + resolverRole, e);
        }
    }
}
