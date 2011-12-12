/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.compiler.jdt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.codehaus.plexus.compiler.CompilerError;
import org.junit.Test;

public class JDTCompilerTest {

    private static final String EOL = System.getProperty("line.separator");

    @Test
    public void testParseModernStreamErrorWithLineAndTrailingSpace() throws IOException {
        List<CompilerError> errors = JDTCompiler.parseModernStream(createOutputForLines(
                "1. ERROR in foo bar (at line 3) ", "2. ERROR in test (at line 5)"));
        assertEquals(2, errors.size());
        CompilerError error = errors.get(0);
        assertTrue(error.isError());
        assertEquals("", error.getMessage());
        assertEquals("foo bar", error.getFile());
        assertEquals(3, error.getStartLine());
        error = errors.get(1);
        assertTrue(error.isError());
        assertEquals("", error.getMessage());
        assertEquals("test", error.getFile());
        assertEquals(5, error.getStartLine());
    }

    @Test
    public void testParseModernStreamContextLines() throws IOException {
        List<CompilerError> errors = JDTCompiler.parseModernStream(createOutputForLines("prologue (must be discarded)",
                "1. ERROR in foo bar (at line 3) ", "message line1", "message line2"));
        assertEquals(1, errors.size());
        CompilerError error = errors.get(0);
        assertEquals(EOL + "message line1" + EOL + "message line2", error.getMessage());
    }

    @Test
    public void testParseModernStreamIgnoreSeparatorLines() throws IOException {
        List<CompilerError> errors = JDTCompiler.parseModernStream(createOutputForLines(//
                "----------",//
                "1. ERROR in foo bar (at line 3) ", //
                "a context line",//
                "----------",//
                "",//
                "2. WARNING in test2 (at line 4)",//
                "second context line",//
                "----------"));
        assertEquals(2, errors.size());
        CompilerError error = errors.get(0);
        assertTrue(error.isError());
        assertEquals("foo bar", error.getFile());
        assertEquals(3, error.getStartLine());
        assertEquals(EOL + "a context line", error.getMessage());
        error = errors.get(1);
        assertFalse(error.isError());
        assertEquals("test2", error.getFile());
        assertEquals(4, error.getStartLine());
        assertEquals(EOL + "second context line", error.getMessage());
    }

    @Test
    public void testParseModernStreamErrorWithoutLine() throws IOException {
        List<CompilerError> errors = JDTCompiler.parseModernStream(createOutputForLines("1. ERROR in baz"));
        assertEquals(1, errors.size());
        CompilerError error = errors.get(0);
        assertTrue(error.isError());
        assertEquals("baz", error.getFile());
        assertEquals(-1, error.getStartLine());
    }

    @Test
    public void testParseModernStreamWarning() throws IOException {
        List<CompilerError> errors = JDTCompiler
                .parseModernStream(createOutputForLines("2. WARNING in foo (at line 4)"));
        assertEquals(1, errors.size());
        CompilerError error = errors.get(0);
        assertFalse(error.isError());
        assertEquals("foo", error.getFile());
        assertEquals(4, error.getStartLine());
    }

    @Test
    public void testParseModernStreamWarningNoNumber() throws IOException {
        List<CompilerError> errors = JDTCompiler.parseModernStream(createOutputForLines("WARNING in foo (at line 4)"));
        assertEquals(1, errors.size());
        CompilerError error = errors.get(0);
        assertFalse(error.isError());
        assertEquals("foo", error.getFile());
        assertEquals(4, error.getStartLine());
    }

    private static BufferedReader createOutputForLines(String... lines) {
        StringBuffer buf = new StringBuffer();
        for (String line : lines) {
            buf.append(line);
            buf.append("\n");
        }
        return new BufferedReader(new StringReader(buf.toString()));
    }
}
