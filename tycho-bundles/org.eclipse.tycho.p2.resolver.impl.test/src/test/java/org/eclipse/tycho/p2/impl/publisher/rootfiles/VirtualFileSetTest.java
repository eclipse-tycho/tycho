/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import org.eclipse.tycho.p2.publisher.rootfiles.VirtualFileSet;
import org.junit.Test;

public class VirtualFileSetTest {

    @Test
    public void testGetMatchingPaths() {
        VirtualFileSet virtualFileSet = new VirtualFileSet("**/*.so*", createTestFileSystem(), true);
        List<IPath> expected = createPathList("foo/bar/test.so", "lib1.so");
        assertEquals(expected, virtualFileSet.getMatchingPaths());
    }

    private List<IPath> createPathList(String... paths) {
        List<IPath> result = new ArrayList<>();
        for (String path : paths) {
            result.add(Path.fromPortableString(path));
        }
        return result;
    }

    private Collection<IPath> createTestFileSystem() {
        Collection<IPath> result = new ArrayList<>();
        String[] paths = new String[] { "foo/bar/test.so", "lib1.so", "testme.txt" };
        for (String path : paths) {
            result.add(Path.fromPortableString(path));
        }
        return result;
    }

}
