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
package org.eclipse.tycho.p2.impl.publisher.rootfiles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RootFilesProperties {

    public class Permission {
        private final String path;

        private final String chmodPermissionPattern;

        public Permission(String path, String chmodPermissionPattern) {
            this.path = path.trim();
            this.chmodPermissionPattern = chmodPermissionPattern;
        }

        public String[] toP2Format() {
            return new String[] { chmodPermissionPattern, path };
        }

    }

    private List<Permission> permissions = new ArrayList<Permission>();

    private StringBuilder links = new StringBuilder();

    public Collection<Permission> getPermissions() {
        return permissions;
    }

    public void addPermission(String chmodPermissionPattern, String[] pathsInInstallation) {
        for (String path : pathsInInstallation) {
            permissions.add(new Permission(path, chmodPermissionPattern));
        }
    }

    public String getLinks() {
        return links.toString();
    }

    public void addLinks(String[] linkValueSegments) {
        verifySpecifiedInPairs(linkValueSegments);
        for (String segment : linkValueSegments) {
            addTrimmedLinkSegment(segment);
        }
    }

    private static void verifySpecifiedInPairs(String[] linkValueSegments) {
        if (linkValueSegments.length % 2 != 0) {
            String message = "Links must be specified as a sequence of \"link target,link name\" pairs; the actual value \""
                    + SegmentHelper.segmentsToString(linkValueSegments, ',') + "\" contains an odd number of segments";
            throw new IllegalArgumentException(message);
        }
    }

    private void addTrimmedLinkSegment(String segment) {
        if (links.length() > 0) {
            links.append(',');
        }
        links.append(segment.trim());
    }
}
