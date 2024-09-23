/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.core.osgitools;

import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.toolchain.ToolchainManager;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.core.DependencyResolver;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.maven.MavenDependenciesResolver;
import org.eclipse.tycho.core.resolver.P2ResolverFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named(PackagingType.TYPE_ECLIPSE_TEST_PLUGIN)
public class OsgiTestBundleProject extends OsgiBundleProject {
    @Inject
    public OsgiTestBundleProject(MavenDependenciesResolver projectDependenciesResolver,
                                 LegacySupport legacySupport,
                                 TychoProjectManager projectManager,
                                 @Named("p2") DependencyResolver dependencyResolver,
                                 BundleReader bundleReader,
                                 ClasspathReader classpathParser,
                                 @Named(EquinoxResolver.HINT) DependenciesResolver resolver,
                                 ToolchainManager toolchainManager,
                                 P2ResolverFactory resolverFactory,
                                 BuildPropertiesParser buildPropertiesParser,
                                 MavenBundleResolver mavenBundleResolver) {
        super(projectDependenciesResolver, legacySupport, projectManager, dependencyResolver, bundleReader, classpathParser, resolver, toolchainManager, resolverFactory, buildPropertiesParser, mavenBundleResolver);
    }
}
