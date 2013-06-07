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
    /**
     * Composes a status message out of the messages of the given status and its recursive children.
     * Children with status severity {@link IStatus#OK} don't add to the constructed message.
     */
    public static String collectProblems(IStatus status) {
        if (!hasChildren(status)) {
            return status.getMessage();
        }

        StringBuilder result = new StringBuilder();
        collectStatusAndChildren(result, status);
        return result.toString();
    }

    private static void collectStatusAndChildren(StringBuilder result, IStatus status) {
        collectStatusMessage(result, status);
        if (hasChildren(status)) {
            result.append(": [");
            collectChildren(result, status.getChildren());
            result.append("]");
        }
    }

    private static void collectStatusMessage(StringBuilder result, IStatus status) {
        result.append(status.getMessage());
    }

    private static void collectChildren(StringBuilder result, IStatus[] children) {
        int trailingSeparatorChars = 0;
        for (IStatus childStatus : children) {
            if (!childStatus.isOK()) {
                collectStatusAndChildren(result, childStatus);
                result.append("; ");
                trailingSeparatorChars = 2;
            }
        }
        result.setLength(result.length() - trailingSeparatorChars);
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
