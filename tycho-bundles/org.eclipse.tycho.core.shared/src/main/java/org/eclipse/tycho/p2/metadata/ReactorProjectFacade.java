/*******************************************************************************
 * Copyright (c) 2020 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.metadata;

import org.eclipse.tycho.ReactorProject;

/**
 * A facade that represents a project in the current reactor
 */
public interface ReactorProjectFacade extends IArtifactFacade {

    ReactorProject getReactorProject();

}
