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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.Test;

public class VirtualFileSetTest {

    @Test
    public void testGetMatchingPaths() {
        VirtualFileSet virtualFileSet = new VirtualFileSet("**/*.so*", createTestFileSystem(), true);
        List<IPath> expected = createPathList("foo/bar/test.so", "lib1.so");
        assertEquals(expected, virtualFileSet.getMatchingPaths());
    }

    private List<IPath> createPathList(String... paths) {
        List<IPath> result = new ArrayList<IPath>();
        for (String path : paths) {
            result.add(Path.fromPortableString(path));
        }
        return result;
    }

    private Collection<IPath> createTestFileSystem() {
        Collection<IPath> result = new ArrayList<IPath>();
        String[] paths = new String[] { "foo/bar/test.so", "lib1.so", "testme.txt" };
        for (String path : paths) {
            result.add(Path.fromPortableString(path));
        }
        return result;
    }

}
