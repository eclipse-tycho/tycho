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
package org.eclipse.tycho.osgi.framework;

import java.nio.file.Path;

/**
 * A generic representation of a workspace that is only initialized once and carries a cache key,
 * belonging to a thread so it can be used in a threadsafe manner
 *
 * @param <T>
 */
public final class EclipseWorkspace<T> {

    private Path workDir;

    private T key;

    private Thread thread;

    EclipseWorkspace(Path workDir, T key, Thread thread) {
        this.workDir = workDir;
        this.key = key;
        this.thread = thread;
    }

    public Path getWorkDir() {
        return workDir;
    }

    public T getKey() {
        return key;
    }

    public Thread getThread() {
        return thread;
    }

}
