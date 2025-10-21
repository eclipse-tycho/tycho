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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.configuration;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.core.shared.MavenLogger;

/**
 * A logger that filters duplicate messages from the output, each message is only printed once
 */
@Named(FilteringMavenLogger.HINT)
@Singleton
public class FilteringMavenLogger implements MavenLogger {

    static final String HINT = "filtering";

    @Inject
    private LegacySupport legacySupport;

    @Inject
    private Logger logger;

    private Map<MavenProject, Set<LogKey>> messageLogMap = new ConcurrentHashMap<>();

    @Override
    public void error(String message, Throwable cause) {
        if (logger.isErrorEnabled() && mustLog(message, cause, 1)) {
            logger.error(message, cause);
        }
    }

    private boolean mustLog(String message, Throwable cause, int type) {
        if (legacySupport == null) {
            return true;
        }
        MavenSession session = legacySupport.getSession();
        if (session == null) {
            return true;
        }
        MavenProject project = session.getCurrentProject();
        if (project == null) {
            return true;
        }
        LogKey logKey;
        if (cause == null) {
            logKey = new LogKey(message, "", type);
        } else {
            logKey = new LogKey(message, cause.toString(), type);
        }
        return messageLogMap.computeIfAbsent(project, p -> ConcurrentHashMap.newKeySet()).add(logKey);
    }

    @Override
    public void warn(String message, Throwable cause) {
        if (logger.isWarnEnabled() && mustLog(message, cause, 2)) {
            logger.warn(message, cause);
        }
    }

    @Override
    public void info(String message) {
        if (logger.isInfoEnabled() && mustLog(message, null, 3)) {
            logger.info(message);
        }
    }

    @Override
    public void debug(String message, Throwable cause) {
        if (logger.isDebugEnabled() && mustLog(message, cause, 4)) {
            logger.warn(message, cause);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public <T> T adapt(Class<T> adapt) {
        if (adapt == Logger.class) {
            return adapt.cast(logger);
        }
        return null;
    }

    private static final record LogKey(String msg, String t, int type) {

    }

}
