/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.publisher.rootfiles;

final class SegmentHelper {
    static boolean segmentEqualsOrIsEndSegment(String[] segments, int segmentIndex, String string) {
        boolean comparingToEndSegment = segments.length == segmentIndex;
        if (string == null) {
            return comparingToEndSegment;
        }
        return segmentEquals(segments, segmentIndex, string);
    }

    static boolean segmentEquals(String[] segments, int segmentIndex, String string) {
        if (segmentIndex < segments.length) {
            return string.equals(segments[segmentIndex]);
        }
        return false;
    }

    static String segmentsToString(String[] keySegments, char separator) {
        if (keySegments.length == 0) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (String segment : keySegments) {
            result.append(segment);
            result.append(separator);
        }
        return result.substring(0, result.length() - 1);
    }

}
