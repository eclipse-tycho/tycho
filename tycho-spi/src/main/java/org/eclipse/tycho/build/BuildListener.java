/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
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
package org.eclipse.tycho.build;

import org.apache.maven.execution.MavenSession;

public interface BuildListener {

    /**
     * Called after the build has started and all projects have basic setup
     * 
     * @param session
     */
    void buildStarted(MavenSession session);

    /**
     * Called after the build has ended
     * 
     * @param session
     */
    void buildEnded(MavenSession session);

}
