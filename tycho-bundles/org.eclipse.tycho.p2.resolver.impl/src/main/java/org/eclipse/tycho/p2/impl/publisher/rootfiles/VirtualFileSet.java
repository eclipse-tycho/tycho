/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.p2.impl.publisher.rootfiles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IPath;

/**
 * Allows to evaluate ant file patterns on a simulated filesystem layout of the resulting
 * installation (as opposed to the filesystem layout during build). Needed for computing chmod
 * permissions applied to files with wildcards.
 */
public class VirtualFileSet extends AbstractFileSet {

    private Collection<IPath> paths;

    public VirtualFileSet(String antFilePattern, Collection<IPath> virtualFileSystem, boolean useDefaultExcludes) {
        super(antFilePattern, useDefaultExcludes);
        this.paths = virtualFileSystem;
    }

    public List<IPath> getMatchingPaths() {
        List<IPath> matchingPaths = new ArrayList<IPath>();
        for (IPath path : paths) {
            if (matches(path)) {
                matchingPaths.add(path);
            }
        }
        return matchingPaths;
    }

}
