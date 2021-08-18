/*******************************************************************************
 * Copyright (c) 2013, 2021 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - #225 MavenLogger is missing error method that accepts an exception
 *******************************************************************************/
package org.eclipse.tycho.core.shared;

/**
 * Delegating {@link MavenLogger} implementation which splits multi-line messages into separate log
 * calls, so that each line is prefixed with a severity.
 */
public class MultiLineLogger implements MavenLogger {

    private final MavenLogger delegate;

    public MultiLineLogger(MavenLogger delegate) {
        this.delegate = delegate;
    }

    @Override
    public void error(String message) {
        for (String messageLine : message.split("\n")) {
            delegate.error(messageLine);
        }
    }

    @Override
    public void error(String message, Throwable cause) {
        String[] messageLines = message.split("\n");

        for (int ix = 0; ix < messageLines.length - 1; ix++) {
            String messageLine = messageLines[ix];
            delegate.error(messageLine);
        }
        if (messageLines.length > 0) {
            String lastMessageLine = messageLines[messageLines.length - 1];
            delegate.error(lastMessageLine, cause);
        }

    }

    public void error(String message, String prefix) {
        for (String messageLine : message.split("\n")) {
            delegate.error(prefix + messageLine);
        }
    }

    @Override
    public void warn(String message) {
        for (String messageLine : message.split("\n")) {
            delegate.warn(messageLine);
        }
    }

    @Override
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

    @Override
    public void info(String message) {
        for (String messageLine : message.split("\n")) {
            delegate.info(messageLine);
        }
    }

    @Override
    public void debug(String message) {
        for (String messageLine : message.split("\n")) {
            delegate.debug(messageLine);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return delegate.isDebugEnabled();
    }

    @Override
    public boolean isExtendedDebugEnabled() {
        return delegate.isExtendedDebugEnabled();
    }

}
