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
package org.eclipse.tycho.p2resolver;

import java.io.File;

import org.eclipse.core.runtime.Path;
import org.eclipse.tycho.p2.publisher.rootfiles.FileSet;
import org.eclipse.tycho.p2.publisher.rootfiles.FileToPathMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FileSetTest {

    @Test
    public void testDoubleStar() {
        FileSet doubleStarAtEnd = new FileSet(null, "test/**");
        Assertions.assertTrue(doubleStarAtEnd.matches(new Path("test/me/foo.txt")));
        Assertions.assertFalse(doubleStarAtEnd.matches(new Path("me/foo.txt")));

        FileSet doubleStarWithSlashAtEnd = new FileSet(null, "test/**/");
        Assertions.assertTrue(doubleStarWithSlashAtEnd.matches(new Path("test/me/foo.txt")));
        Assertions.assertFalse(doubleStarWithSlashAtEnd.matches(new Path("me/foo.txt")));

        FileSet doubleStarAtBeginning = new FileSet(null, "**/FILE");
        Assertions.assertTrue(doubleStarAtBeginning.matches(new Path("test/me/FILE")));

        FileSet doubleStarAtBeginningAndEnd = new FileSet(null, "**/DIR/**");
        Assertions.assertTrue(doubleStarAtBeginningAndEnd.matches(new Path("test/me/DIR/bar/test.txt")));
        Assertions.assertFalse(doubleStarAtBeginningAndEnd.matches(new Path("test/me/foobar/test.txt")));
        Assertions.assertFalse(doubleStarAtBeginningAndEnd.matches(new Path("test/me/DIR")));
    }

    @Test
    public void testSingleStar() {
        FileSet starAtBeginning = new FileSet(null, "*.txt");
        Assertions.assertTrue(starAtBeginning.matches(new Path("foo.txt")));

        FileSet starAtEnd = new FileSet(null, "bar*");
        Assertions.assertTrue(starAtEnd.matches(new Path("barfoo")));
        Assertions.assertFalse(starAtEnd.matches(new Path("foobar")));

        FileSet starInMiddle = new FileSet(null, "bar*foo");
        Assertions.assertTrue(starInMiddle.matches(new Path("bar_test_foo")));
        Assertions.assertFalse(starInMiddle.matches(new Path("bar_test_fooX")));
    }

    @Test
    public void testQuestionMark() {
        FileSet questionMarkPattern = new FileSet(null, "foo?.txt");
        Assertions.assertTrue(questionMarkPattern.matches(new Path("fooX.txt")));
        Assertions.assertFalse(questionMarkPattern.matches(new Path("fooXY.txt")));
        Assertions.assertFalse(questionMarkPattern.matches(new Path("XfooY.txt")));
    }

    @Test
    public void testCombined() {
        FileSet recursiveTxtPattern = new FileSet(null, "**/*.txt");
        Assertions.assertTrue(recursiveTxtPattern.matches(new Path("tmp/foo.txt")));
        Assertions.assertTrue(recursiveTxtPattern.matches(new Path("foo.txt")));
        Assertions.assertFalse(recursiveTxtPattern.matches(new Path("foo.txt_")));
        FileSet recursiveFilePrefixPattern = new FileSet(null, "**/prefix*");
        Assertions.assertTrue(recursiveFilePrefixPattern.matches(new Path("tmp/prefixfoo.txt")));
    }

    @Test
    public void testDefaultExcludes() {
        FileSet recursiveFileSet = new FileSet(null, "test/**");
        Assertions.assertTrue(recursiveFileSet.matches(new Path("test/me/foo.txt")));
        Assertions.assertFalse(recursiveFileSet.matches(new Path("test/CVS/foo.txt")));
        Assertions.assertFalse(recursiveFileSet.matches(new Path("test/.git/foo.txt")));
        Assertions.assertFalse(recursiveFileSet.matches(new Path("test/.svn/foo.txt")));
        Assertions.assertFalse(recursiveFileSet.matches(new Path("test/me/.svn")));
    }

    @Test
    public void testNoDefaultExcludes() {
        FileSet recursiveFileSet = new FileSet(null, "test/**", "", false);
        Assertions.assertTrue(recursiveFileSet.matches(new Path("test/CVS/foo.txt")));
        Assertions.assertTrue(recursiveFileSet.matches(new Path("test/.git/foo.txt")));
        Assertions.assertTrue(recursiveFileSet.matches(new Path("test/.svn/foo.txt")));
    }

    @Test
    public void testScan() {
        FileSet txtFileset = new FileSet(new File("src/test/resources/rootfiles"), "**/*.txt");
        FileToPathMap result = txtFileset.scan();
        Assertions.assertEquals(4, result.keySet().size());
    }
}
