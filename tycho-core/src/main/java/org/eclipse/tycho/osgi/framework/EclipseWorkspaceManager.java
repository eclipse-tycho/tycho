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

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;

/**
 * The {@link EclipseWorkspaceManager} manages dedicated workspaces on a per thread basis using a
 * key object
 */
@Component(role = EclipseWorkspaceManager.class)
public class EclipseWorkspaceManager implements Disposable {

    private final Map<Thread, Map<Object, EclipseWorkspace<?>>> cache = new ConcurrentHashMap<>();

    @Requirement
    private Logger logger;

    /**
     * @param key
     *            the key to use
     * @return a workspace directory that can be used by the current thread.
     */
    @SuppressWarnings("unchecked")
    public <T> EclipseWorkspace<T> getWorkspace(T key) {
        Thread currentThread = Thread.currentThread();
        return (EclipseWorkspace<T>) cache.computeIfAbsent(currentThread, t -> new ConcurrentHashMap<>())
                .computeIfAbsent(key, x -> {
                    try {
                        return new EclipseWorkspace<>(Files.createTempDirectory("eclipseWorkspace"), key, logger,
                                currentThread);
                    } catch (IOException e) {
                        throw new IllegalStateException("can't create a temporary directory for the workspace!", e);
                    }
                });
    }

    @Override
    public void dispose() {
        cache.values().forEach(map -> {
            map.values().forEach(ws -> FileUtils.deleteQuietly(ws.getWorkDir().toFile()));
        });
    }

}
