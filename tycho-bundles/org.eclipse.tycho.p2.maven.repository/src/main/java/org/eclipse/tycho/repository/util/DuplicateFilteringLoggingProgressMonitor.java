/*******************************************************************************
 * Copyright (c) 2013, 2020 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - filter additional string
 *******************************************************************************/
package org.eclipse.tycho.repository.util;

import java.util.regex.Pattern;

import org.eclipse.tycho.core.shared.MavenLogger;

/**
 * {@link LoggingProgressMonitor} which removes duplicated and other obsolete log output produced by
 * p2/ECF when downloading artifacts.
 * 
 * <p>
 * Instances of this class are not thread-safe.
 * </p>
 */
public final class DuplicateFilteringLoggingProgressMonitor extends LoggingProgressMonitor {

    private static final String NON_MATCHING_LINE = "";
    private String lastLine = NON_MATCHING_LINE;
    private static final Pattern PATTERN_FETCHING = Pattern.compile("Fetching \\S+ from");

    public DuplicateFilteringLoggingProgressMonitor(MavenLogger logger) {
        super(logger);
    }

    @Override
    protected boolean suppressOutputOf(String text) {
        if (text.equals("1 operation remaining.")) {
            // filter out
            return true;
        }
        if (text.equals("Performing subquery")) {
            return true;
        }
        boolean isUnneededLine = checkIfDuplicateOfLastOutput(text);
        return isUnneededLine;
    }

    private boolean checkIfDuplicateOfLastOutput(String message) {
        // special handling for "Fetching %file from %url ..." lines
        if (!PATTERN_FETCHING.matcher(message).find()) {
            lastLine = NON_MATCHING_LINE;
            return false;
        }
        boolean duplicate = message.equals(lastLine);
        lastLine = message;
        return duplicate;
    }

}
