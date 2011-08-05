/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - added verification functionality
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.test;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.tycho.core.facade.MavenLogger;

public class MavenLoggerStub implements MavenLogger {

    private final boolean failOnWarning;
    private final List<String> warnings = new ArrayList<String>();

    public MavenLoggerStub() {
        this(false);
    }

    public MavenLoggerStub(boolean failOnWarning) {
        this.failOnWarning = failOnWarning;
    }

    public void warn(String message) {
        warn(message, null);
    }

    public void warn(String message, Throwable cause) {
        if (failOnWarning)
            throw new RuntimeException("Unexpected warning: " + message, cause);
        warnings.add(message);
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void info(String message) {
    }

    public void debug(String message) {
    }

    public boolean isExtendedDebugEnabled() {
        // run through message preparation code in tests
        return true;
    }

    public boolean isDebugEnabled() {
        return true;
    }
}
