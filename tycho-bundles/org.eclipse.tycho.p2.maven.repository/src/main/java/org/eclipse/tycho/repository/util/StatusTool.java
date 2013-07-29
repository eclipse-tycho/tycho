/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.util;

import org.eclipse.core.runtime.IStatus;

public class StatusTool {

    private static class StatusStringBuilder {
        private StringBuilder result = new StringBuilder();

        void appendStatusAndChildren(IStatus status, String indentation) {
            appendStatusMessage(status);

            if (hasChildren(status)) {
                appendChildren(status, indentation);
            }
        }

        private void appendStatusMessage(IStatus status) {
            result.append(status.getMessage());
        }

        private void appendChildren(IStatus status, String indentation) {

            if (indentation == null) {
                result.append(": [");
                appendChildList(status.getChildren(), null);
                result.append("]");

            } else {
                result.append(":\n");
                appendChildList(status.getChildren(), indentation + "   ");
            }
        }

        private void appendChildList(IStatus[] children, String indentation) {
            int trailingSeparatorChars = 0;

            for (IStatus childStatus : children) {
                if (!childStatus.isOK()) {
                    trailingSeparatorChars = appendChild(childStatus, indentation);
                }
            }
            result.setLength(result.length() - trailingSeparatorChars);
        }

        private int appendChild(IStatus status, String indentation) {

            if (indentation == null) {
                appendStatusAndChildren(status, indentation);
                return appendAndReturnLength("; ");

            } else {
                result.append(indentation);
                appendStatusAndChildren(status, indentation);
                return appendAndReturnLength("\n");
            }
        }

        private int appendAndReturnLength(String string) {
            result.append(string);
            return string.length();
        }

        @Override
        public String toString() {
            return result.toString();
        }
    }

    /**
     * Composes a single line message out of the messages of the status and its recursive children.
     * Children with status severity {@link IStatus#OK} don't add to the constructed message.
     */
    public static String collectProblems(IStatus status) {
        return statusToString(status, false);
    }

    /**
     * Converts to given status to a log message. If the status is a multi-status, each child will
     * be written on a separate line, showing the hierarchy will through indentation.
     */
    public static String toLogMessage(IStatus status) {
        return statusToString(status, true);
    }

    private static String statusToString(IStatus status, boolean multiLine) {
        if (!hasChildren(status)) {
            return status.getMessage();
        }

        StatusStringBuilder builder = new StatusStringBuilder();
        String indentation = multiLine ? "" : null;
        builder.appendStatusAndChildren(status, indentation);
        return builder.toString();
    }

    public static Throwable findException(IStatus status) {
        Throwable rootException = status.getException();
        if (rootException != null)
            return rootException;
        if (hasChildren(status)) {
            return findExceptionInChildren(status.getChildren());
        }
        return null;
    }

    private static Throwable findExceptionInChildren(IStatus[] children) {
        for (IStatus child : children) {
            Throwable childException = findException(child);
            if (childException != null)
                return childException;
        }
        return null;
    }

    private static boolean hasChildren(IStatus status) {
        return status.getChildren() != null && status.getChildren().length > 0;
    }
}
