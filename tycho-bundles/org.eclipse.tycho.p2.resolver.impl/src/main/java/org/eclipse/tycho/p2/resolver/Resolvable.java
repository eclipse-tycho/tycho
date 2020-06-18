/*******************************************************************************
 * Copyright (c) 2020 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.p2.util.resolution.ResolverException;

public interface Resolvable {

    /**
     * resolves and returns the result
     * 
     * @param monitor
     * 
     * @return the (possibly empty) result of the resolve operation
     */
    Collection<IInstallableUnit> resolve(IProgressMonitor monitor) throws ResolverException;
}
