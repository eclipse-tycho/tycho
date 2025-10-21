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
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.Mojo;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.PreDestroy;

@Named
@Singleton
public class DefaultEclipseWorkspaceManager implements EclipseWorkspaceManager {

    private final Map<Thread, Map<Object, EclipseWorkspace<?>>> cache = new WeakHashMap<>();
    private final List<EclipseWorkspace<?>> toclean = new ArrayList<>();

    @Inject
    private Logger logger;

    /**
     * @param key
     *            the key to use
     * @return a workspace directory that can be used by the current thread.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> EclipseWorkspace<T> getWorkspace(T key) {
        Thread currentThread = Thread.currentThread();
        synchronized (cache) {
            return (EclipseWorkspace<T>) cache.computeIfAbsent(currentThread, t -> new ConcurrentHashMap<>())
                    .computeIfAbsent(key, x -> {
                        try {
                            EclipseWorkspace<T> workspace = new EclipseWorkspace<>(
                                    Files.createTempDirectory("eclipseWorkspace"), key, currentThread);
                            toclean.add(workspace);
                            return workspace;
                        } catch (IOException e) {
                            throw new IllegalStateException("can't create a temporary directory for the workspace!", e);
                        }
                    });
        }
    }

    @PreDestroy
    public void dispose() {
        cache.clear();
        for (EclipseWorkspace<?> workspace : toclean) {
            FileUtils.deleteQuietly(workspace.getWorkDir().toFile());
        }
    }

    /**
     * Get a workspace that is unique for the given uri, current thread and mojo and therefore safe
     * to be used in a maven multithread execution
     * 
     * @param uri
     * @param mojo
     * @return an {@link EclipseWorkspace}
     */
    @Override
    public EclipseWorkspace<?> getWorkspace(URI uri, Mojo mojo) {
        return getWorkspace(new MojoKey(uri.normalize(), mojo.getClass().getName()));

    }

    private static final record MojoKey(URI uri, String mojoClassName) {
        //a key that uses the mojo class and a URI
    }

}
