/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.test.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.tycho.core.facade.MavenLogger;

public class MemoryLog implements MavenLogger {
    private final boolean failOnError;

    public final List<String> warnings = new ArrayList<String>();
    public final List<String> errors = new ArrayList<String>();

    public MemoryLog(boolean failOnError) {
        this.failOnError = failOnError;
    }

    public void error(String message) {
        if (failOnError)
            throw new RuntimeException(message);
        else
            errors.add(message);
    }

    public void warn(String message) {
        warnings.add(message);
    }

    public void warn(String message, Throwable cause) {
        warnings.add(message);
    }

    public void info(String message) {
    }

    public void debug(String message) {
    }

    public boolean isDebugEnabled() {
        return false;
    }

    public boolean isExtendedDebugEnabled() {
        return false;
    }
}
