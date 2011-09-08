/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.adapters;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.core.facade.MavenLogger;

public class MavenLoggerAdapter implements MavenLogger {

    private final Logger logger;
    private final boolean extendedDebug;

    public MavenLoggerAdapter(Logger logger, boolean extendedDebug) {
        this.logger = logger;
        this.extendedDebug = extendedDebug;
    }

    public void debug(String message) {
        if (!isEmpty(message)) {
            logger.debug(message);
        }
    }

    public void info(String message) {
        if (!isEmpty(message)) {
            logger.info(message);
        }
    }

    public void warn(String message) {
        warn(message, null);
    }

    public void warn(String message, Throwable cause) {
        if (!isEmpty(message)) {
            logger.warn(message, cause);
        }
    }

    public void error(String message) {
        logger.error(message);
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public boolean isExtendedDebugEnabled() {
        return isDebugEnabled() && extendedDebug;
    }

    private boolean isEmpty(String message) {
        return message == null || message.length() == 0;
    }
}
