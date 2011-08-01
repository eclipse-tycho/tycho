/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.util;

import org.eclipse.core.runtime.IStatus;

public class StatusTool {
    /**
     * Composes a status message out of the messages of the given status and its recursive children.
     * Children with status severity {@link IStatus#OK} don't add to the constructed message.
     */
    public static String collectProblems(IStatus status) {
        if (status.getChildren() == null || status.getChildren().length == 0) {
            return status.getMessage();
        }

        StringBuilder result = new StringBuilder();
        collectStatusAndChildren(result, status);
        return result.toString();
    }

    private static void collectStatusAndChildren(StringBuilder result, IStatus status) {
        collectStatusMessage(result, status);
        IStatus[] children = status.getChildren();
        if (children != null && children.length > 0) {
            result.append(": [");
            collectChildren(result, children);
            result.append("]");
        }
    }

    private static void collectStatusMessage(StringBuilder result, IStatus status) {
        result.append('"');
        result.append(status.getMessage());
        result.append('"');
    }

    private static void collectChildren(StringBuilder result, IStatus[] children) {
        int trailingSeparatorChars = 0;
        for (IStatus childStatus : children) {
            if (!childStatus.isOK()) {
                collectStatusAndChildren(result, childStatus);
                result.append(", ");
                trailingSeparatorChars = 2;
            }
        }
        result.setLength(result.length() - trailingSeparatorChars);
    }
}
