/*******************************************************************************
 * Copyright (c) 2008, 2013 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver.facade;

import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.p2.target.facade.TargetPlatformFactory;

public interface P2ResolverFactory {

    /**
     * Creates a new object for collecting the bundles within the POM dependencies.
     */
    // TODO move to a PomDependencyCollectorFactory interface?
    public PomDependencyCollector newPomDependencyCollector();

    // TODO directly register as service
    public TargetPlatformFactory getTargetPlatformFactory();

    public P2Resolver createResolver(MavenLogger logger);
}
