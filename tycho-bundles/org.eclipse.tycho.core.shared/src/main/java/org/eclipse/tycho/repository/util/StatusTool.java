/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.util;

import org.eclipse.core.runtime.IStatus;

public class StatusTool {

    private static class StatusStringBuilder {
        private StringBuilder result = new StringBuilder();

        void appendStatusAndChildren(IStatus status, HierarchyFormatter formatter) {
            appendStatusMessage(status, formatter);

            if (hasChildren(status)) {
                appendChildren(status, formatter);
            }
        }

        private void appendStatusMessage(IStatus status, HierarchyFormatter formatter) {
            result.append(formatter.indentationString);
            result.append(status.getMessage());
        }

        private void appendChildren(IStatus status, HierarchyFormatter formatter) {
            result.append(formatter.childrenStartString);
            appendStatusList(status.getChildren(), formatter.getChildLevelFormatter());
            result.append(formatter.childrenEndString);
        }

        private void appendStatusList(IStatus[] statusList, HierarchyFormatter formatter) {
            boolean trailingSeparator = false;

            for (IStatus status : statusList) {
                if (!status.isOK()) {
                    appendStatusAndChildren(status, formatter);

                    result.append(formatter.listSeparatorString);
                    trailingSeparator = true;
                }
            }

            if (trailingSeparator) {
                result.setLength(result.length() - formatter.listSeparatorString.length());
            }
        }

        @Override
        public String toString() {
            return result.toString();
        }
    }

    private static class HierarchyFormatter {

        final private String indentationIncrement;
        final String indentationString;

        final String childrenStartString;
        final String childrenEndString;

        final String listSeparatorString;

        HierarchyFormatter(String childrenStart, String indentationIncrement, String listSeparator, String childrenEnd) {
            this("", childrenStart, indentationIncrement, listSeparator, childrenEnd);
        }

        private HierarchyFormatter(String indentation, String childrenStart, String indentationIncrement,
                String listSeparator, String childrenEnd) {
            this.indentationString = indentation;
            this.indentationIncrement = indentationIncrement;

            this.childrenStartString = childrenStart;
            this.childrenEndString = childrenEnd;

            this.listSeparatorString = listSeparator;
        }

        public HierarchyFormatter getChildLevelFormatter() {
            if (indentationIncrement == null) {
                // child level is formatted in the same way as current level
                return this;
            } else {
                // return formatter with increased indentation
                return new HierarchyFormatter(indentationString + indentationIncrement, childrenStartString,
                        indentationIncrement, listSeparatorString, childrenEndString);
            }
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
        HierarchyFormatter formatter;
        if (multiLine)
            formatter = new HierarchyFormatter(":\n", "   ", "\n", "");
        else
            formatter = new HierarchyFormatter(": [", null, "; ", "]");
        builder.appendStatusAndChildren(status, formatter);
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
