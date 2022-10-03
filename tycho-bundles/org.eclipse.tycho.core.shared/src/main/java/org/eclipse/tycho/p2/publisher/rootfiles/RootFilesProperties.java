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
package org.eclipse.tycho.p2.publisher.rootfiles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import org.eclipse.core.runtime.IPath;

public class RootFilesProperties {

    public class Permission {
        private final String pathPattern;
        private Set<String> resolvedPaths;

        // 3-digit octal file permission mask 
        private final String chmodPermissions;
        private boolean isResolved = false;

        public Permission(String pathPattern, String chmodPermissions) {
            this.pathPattern = pathPattern;
            this.chmodPermissions = chmodPermissions;
        }

        void resolveWildcards(Collection<IPath> virtualFiles, boolean useDefaultExcludes) {
            resolvedPaths = new HashSet<>();
            VirtualFileSet virtualFileSet = new VirtualFileSet(pathPattern, virtualFiles, useDefaultExcludes);
            for (IPath path : virtualFileSet.getMatchingPaths()) {
                resolvedPaths.add(path.toString());
            }
            isResolved = true;
        }

        public List<String[]> toP2Formats() {
            if (!isResolved) {
                throw new IllegalStateException("must call resolveWildcards() first");
            }
            List<String[]> p2Formats = new ArrayList<>(resolvedPaths.size());
            for (String resolvedPath : resolvedPaths) {
                String[] p2Format = new String[] { chmodPermissions, resolvedPath };
                p2Formats.add(p2Format);
            }
            return p2Formats;
        }
    }

    /**
     * Absolute source location of a root file to the relative pathPattern that describes the
     * location of the root file in the installed product.
     */
    private FileToPathMap fileSourceToDestinationMap = new FileToPathMap();

    private List<Permission> permissions = new ArrayList<>();

    private StringJoiner links = new StringJoiner(",");

    public FileToPathMap getFileMap() {
        return fileSourceToDestinationMap;
    }

    public void addFiles(FileToPathMap fileSourceToDestinationMap) {
        this.fileSourceToDestinationMap.putAll(fileSourceToDestinationMap);
    }

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
            links.add(segment);
        }
    }

    public void resolvePermissionWildcards(boolean useDefaultExcludes) {
        Collection<IPath> allFilePaths = fileSourceToDestinationMap.values();
        for (Permission permission : permissions) {
            permission.resolveWildcards(allFilePaths, useDefaultExcludes);
        }
    }

    private static void verifySpecifiedInPairs(String[] linkValueSegments) {
        if (linkValueSegments.length % 2 != 0) {
            String message = "Links must be specified as a sequence of \"link target,link name\" pairs; the actual value \""
                    + String.join(",", linkValueSegments) + "\" contains an odd number of segments";
            throw new IllegalArgumentException(message);
        }
    }
}
