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
package org.eclipse.tycho.p2.impl.resolver;

import java.io.File;

import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.resolver.facade.ResolutionContext;
import org.eclipse.tycho.p2.resolver.impl.ResolutionContextImpl;

public class P2ResolverFactoryImpl implements P2ResolverFactory {

    public ResolutionContext createResolutionContext(File localMavenRepositoryRoot, MavenLogger logger) {
        return new ResolutionContextImpl(localMavenRepositoryRoot, logger);
    }

    public P2Resolver createResolver(MavenLogger logger) {
        return new P2ResolverImpl(logger);
    }

}
