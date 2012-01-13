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
package org.eclipse.tycho.p2.metadata;

import java.util.Set;

public interface IReactorArtifactFacade extends IArtifactFacade {
    /**
     * Conventional sources jar bundle symbolic name suffix.
     */
    public static final String SOURCE_BUNDLE_SUFFIX = ".source";

    /**
     * Returns primary or secondary IInstallableUnits provided by the reactor project, never
     * <code>null</code>.
     * <p/>
     * Primary IUs are used to determine project dependencies. Secondary IUs can be used to resolve
     * dependencies of other reactor projects but do not affect dependencies of this project unless
     * required by the primary IUs.
     */
    public Set<Object/* IInstallableUnit */> getDependencyMetadata(boolean primary);
}
