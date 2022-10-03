/*******************************************************************************
 * Copyright (c) 2011, 2017 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Bachmann electronic GmbH - adding support for root.folder and root.<config>.folder
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.publisher.rootfiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.tycho.p2.publisher.rootfiles.FileSet;
import org.eclipse.tycho.p2.publisher.rootfiles.RootFilePatternParser.RootFilePath;
import org.junit.Test;

public class RootFilePathTest {

    @Test
    public void testAbsoluteFile() {
        RootFilePath rootFilePath = new RootFilePath("absolute:file:/tmp/test.txt", new File("/basedir"), "");
        FileSet fileSet = rootFilePath.toFileSet(true);
        assertTrue(fileSet.matches(new Path("test.txt")));
        IPath normalizedBaseDir = Path.fromOSString(fileSet.getBaseDir().getAbsolutePath()).setDevice(null);
        assertEquals(new Path("/tmp"), normalizedBaseDir);
    }

    @Test
    public void testRelativeFile() {
        RootFilePath rootFilePath = new RootFilePath("file:foo/test.txt", new File("/basedir"), "");
        FileSet fileSet = rootFilePath.toFileSet(true);
        assertTrue(fileSet.matches(new Path("test.txt")));
        assertFalse(fileSet.matches(new Path("second_test.txt")));
        IPath normalizedBaseDir = Path.fromOSString(fileSet.getBaseDir().getAbsolutePath()).setDevice(null);
        assertEquals(new Path("/basedir/foo"), normalizedBaseDir);
    }

    @Test
    public void testAbsoluteDir() {
        RootFilePath rootFilePath = new RootFilePath("absolute:/tmp/bar/", new File("/basedir"), "");
        FileSet fileSet = rootFilePath.toFileSet(true);
        assertTrue(fileSet.matches(new Path("anyfile")));
        IPath normalizedBaseDir = Path.fromOSString(fileSet.getBaseDir().getAbsolutePath()).setDevice(null);
        assertEquals(new Path("/tmp/bar"), normalizedBaseDir);
    }

    @Test
    public void testRelativeDir() {
        RootFilePath rootFilePath = new RootFilePath("foo/baz/", new File("/basedir"), "");
        FileSet fileSet = rootFilePath.toFileSet(true);
        assertTrue(fileSet.matches(new Path("anyfileName")));
        IPath normalizedBaseDir = Path.fromOSString(fileSet.getBaseDir().getAbsolutePath()).setDevice(null);
        assertEquals(new Path("/basedir/foo/baz/"), normalizedBaseDir);
    }

}
