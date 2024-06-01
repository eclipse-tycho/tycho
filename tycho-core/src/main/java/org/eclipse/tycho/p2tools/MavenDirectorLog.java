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
package org.eclipse.tycho.p2tools;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.tycho.core.shared.StatusTool;
import org.eclipse.tycho.p2tools.copiedfromp2.ILog;

public class MavenDirectorLog implements ILog {

    private String name;
    private Log logger;

    public MavenDirectorLog(String name, Log logger) {
        this.name = name;
        this.logger = logger;
    }

    @Override
    public void log(IStatus status) {
        String message = getMsgLine(StatusTool.toLogMessage(status));
        if (status.getSeverity() == IStatus.ERROR) {
            logger.error(message, status.getException());
        } else if (status.getSeverity() == IStatus.WARNING) {
            logger.warn(message);
        } else if (status.getSeverity() == IStatus.INFO) {
            logger.info(message);
        } else {
            logger.debug(message);
        }

    }

    @Override
    public void printOut(String line) {
        logger.info(getMsgLine(line));
    }

    private String getMsgLine(String line) {
        return "[" + name + "] " + line;
    }

    @Override
    public void printErr(String line) {
        logger.error(getMsgLine(line));
    }

}
