/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.facade;

/**
 * Delegating {@link MavenLogger} implementation which splits multi-line messages into separate log
 * calls, so that each line is prefixed with a severity.
 */
public class MultiLineLogger implements MavenLogger {

    private final MavenLogger delegate;

    public MultiLineLogger(MavenLogger delegate) {
        this.delegate = delegate;
    }

    public void error(String message) {
        for (String messageLine : message.split("\n")) {
            delegate.error(messageLine);
        }
    }

    public void warn(String message) {
        for (String messageLine : message.split("\n")) {
            delegate.warn(messageLine);
        }
    }

    public void warn(String message, Throwable cause) {
        String[] messageLines = message.split("\n");

        for (int ix = 0; ix < messageLines.length - 1; ix++) {
            String messageLine = messageLines[ix];
            delegate.warn(messageLine);
        }
        if (messageLines.length > 0) {
            String lastMessageLine = messageLines[messageLines.length - 1];
            delegate.warn(lastMessageLine, cause);
        }
    }

    public void info(String message) {
        for (String messageLine : message.split("\n")) {
            delegate.info(messageLine);
        }
    }

    public void debug(String message) {
        for (String messageLine : message.split("\n")) {
            delegate.debug(messageLine);
        }
    }

    public boolean isDebugEnabled() {
        return delegate.isDebugEnabled();
    }

    public boolean isExtendedDebugEnabled() {
        return delegate.isExtendedDebugEnabled();
    }

}
