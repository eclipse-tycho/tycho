/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - Bug 567098 - pomDependencies=consider should wrap non-osgi jars
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver.facade;

import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.p2.target.facade.TargetPlatformFactory;

public interface P2ResolverFactory {

    /**
     * Creates a new object for collecting the bundles within the POM dependencies.
     */
    // TODO move to a PomDependencyCollectorFactory interface?
    public PomDependencyCollector newPomDependencyCollector(ReactorProject project);

    // TODO directly register as service
    public TargetPlatformFactory getTargetPlatformFactory();

    public P2Resolver createResolver(MavenLogger logger);
}
