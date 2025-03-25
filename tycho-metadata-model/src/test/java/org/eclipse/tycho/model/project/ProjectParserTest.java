/*******************************************************************************
 * Copyright (c) 2025 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.model.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

import org.eclipse.tycho.model.project.ProjectParser.LinkDescription;
import org.junit.jupiter.api.Test;

public class ProjectParserTest {

    private static final Path PROJECT_PATH = Path.of("/path/to/project");

    private static final Map<String, URI> VARS = Map.of( //
            "FOO1", URI.create("$%7BBAR1%7D/foo"), //
            "BAR1", URI.create("$%7BBAZ1%7D/bar"), //
            "BAZ1", URI.create("$%7BPROJECT_LOC%7D/other/dir"), //

            "FOO2", URI.create("$%7BBAR2%7D/foo"), //
            "BAR2", URI.create("$%7BBAZ2%7D/bar"), //
            "BAZ2", URI.create("$%7BPARENT-1-PROJECT_LOC%7D/otherproject/dir"), //

            "RECURSION1", URI.create("$%7BRECURSION2%7D/foo"), //
            "RECURSION2", URI.create("$%7BRECURSION1%7D/foo"), //

            "LITERAL", URI.create("value"), //

            "UNKNOWN", URI.create("$%7BDONTKNOW%7D/foo") //
    );

    @Test
    void testLinkNoLocationOrUri() throws Exception {
        LinkDescription link = new LinkDescription(Path.of("link"), LinkDescription.FOLDER, null, null);
        Path result = ProjectParser.resolvePath(link, Path.of("link/subdir"), PROJECT_PATH, VARS);
        assertNull(result);
    }

    @Test
    void testLinkDirSimpleCase() throws Exception {
        LinkDescription link = new LinkDescription(Path.of("link"), LinkDescription.FOLDER, null,
                URI.create("foo/bar"));
        Path result = ProjectParser.resolvePath(link, Path.of("link/subdir"), PROJECT_PATH, VARS);
        assertEquals(Path.of("/path/to/project/foo/bar/subdir"), result);
    }

    @Test
    void testLinkDirProjectLoc() throws Exception {
        LinkDescription link = new LinkDescription(Path.of("link"), LinkDescription.FOLDER, null,
                URI.create("PROJECT_LOC/bar"));
        Path result = ProjectParser.resolvePath(link, Path.of("link/subdir"), PROJECT_PATH, VARS);
        assertEquals(Path.of("/path/to/project/bar/subdir"), result);
    }

    @Test
    void testLinkDirParent1ProjectLoc() throws Exception {
        LinkDescription link = new LinkDescription(Path.of("link"), LinkDescription.FOLDER, null,
                URI.create("PARENT-1-PROJECT_LOC/otherproject"));
        Path result = ProjectParser.resolvePath(link, Path.of("link/subdir"), PROJECT_PATH, VARS);
        assertEquals(Path.of("/path/to/otherproject/subdir"), result);
    }

    @Test
    void testLinkDirParent2ProjectLoc() throws Exception {
        LinkDescription link = new LinkDescription(Path.of("link"), LinkDescription.FOLDER, null,
                URI.create("PARENT-2-PROJECT_LOC/anotherdir"));
        Path result = ProjectParser.resolvePath(link, Path.of("link/subdir"), PROJECT_PATH, VARS);
        assertEquals(Path.of("/path/anotherdir/subdir"), result);
    }

    @Test
    void testLinkDirVarExpandEndingInProjectLoc() throws Exception {
        LinkDescription link = new LinkDescription(Path.of("link"), LinkDescription.FOLDER, null, URI.create("FOO1/x"));
        Path result = ProjectParser.resolvePath(link, Path.of("link/subdir"), PROJECT_PATH, VARS);
        assertEquals(Path.of("/path/to/project/other/dir/bar/foo/x/subdir"), result);
    }

    @Test
    void testLinkDirVarExpandEndingInParent1ProjectLoc() throws Exception {
        LinkDescription link = new LinkDescription(Path.of("link"), LinkDescription.FOLDER, null, URI.create("FOO2/x"));
        Path result = ProjectParser.resolvePath(link, Path.of("link/subdir"), PROJECT_PATH, VARS);
        assertEquals(Path.of("/path/to/otherproject/dir/bar/foo/x/subdir"), result);
    }

    @Test
    void testLinkDirRecursion() throws Exception {
        LinkDescription link = new LinkDescription(Path.of("link"), LinkDescription.FOLDER, null,
                URI.create("RECURSION1/x"));
        Path result = ProjectParser.resolvePath(link, Path.of("link/subdir"), PROJECT_PATH, VARS);
        assertNull(result);
    }

    @Test
    void testLinkDirVarExpandLiteral() throws Exception {
        LinkDescription link = new LinkDescription(Path.of("link"), LinkDescription.FOLDER, null,
                URI.create("LITERAL/x"));
        Path result = ProjectParser.resolvePath(link, Path.of("link/subdir"), PROJECT_PATH, VARS);
        assertEquals(Path.of("/path/to/project/value/x/subdir"), result);
    }

    @Test
    void testLinkDirVarExpandFailingKeepDollarVariable() throws Exception {
        LinkDescription link = new LinkDescription(Path.of("link"), LinkDescription.FOLDER, null,
                URI.create("UNKNOWN/x"));
        Path result = ProjectParser.resolvePath(link, Path.of("link/subdir"), PROJECT_PATH, VARS);
        assertEquals(Path.of("/path/to/project/${DONTKNOW}/foo/x/subdir"), result);
    }

    @Test
    void testLinkDirAbsolutePathInsteadOfUri() throws Exception {
        LinkDescription link = new LinkDescription(Path.of("link"), LinkDescription.FOLDER, Path.of("/absolute/path"),
                null);
        Path result = ProjectParser.resolvePath(link, Path.of("link/subdir"), PROJECT_PATH, VARS);
        assertEquals(Path.of("/absolute/path/subdir"), result);
    }

    @Test
    void testLinkFileSimpleCase() throws Exception {
        LinkDescription link = new LinkDescription(Path.of("link.txt"), LinkDescription.FILE, null,
                URI.create("linktarget.txt"));
        Path result = ProjectParser.resolvePath(link, Path.of("link.txt"), PROJECT_PATH, VARS);
        assertEquals(Path.of("/path/to/project/linktarget.txt"), result);
    }

    @Test
    void testLinkFileAbsolutePathInsteadOfUri() throws Exception {
        LinkDescription link = new LinkDescription(Path.of("link.txt"), LinkDescription.FILE,
                Path.of("/path/to/some/txtfile.txt"), null);
        Path result = ProjectParser.resolvePath(link, Path.of("link.txt"), PROJECT_PATH, VARS);
        assertEquals(Path.of("/path/to/some/txtfile.txt"), result);
    }

    @Test
    void testLinkFileIllegalSegmentAfterFile() throws Exception {
        LinkDescription link = new LinkDescription(Path.of("link.txt"), LinkDescription.FILE,
                Path.of("/path/to/some/txtfile.txt"), null);
        Path result = ProjectParser.resolvePath(link, Path.of("link.txt/thisisinvalid"), PROJECT_PATH, VARS);
        assertNull(result);
    }

}
