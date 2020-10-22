/*******************************************************************************
 * Copyright (c) 2011, 2020 SAP AG and others.
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

package org.eclipse.tycho.compiler.jdt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerMessage.Kind;
import org.junit.jupiter.api.Test;

public class JDTCompilerTest {

    private static final String EOL = System.lineSeparator();

    @Test
    public void testParseModernStreamErrorWithLineAndTrailingSpace() throws IOException {
        List<CompilerMessage> messages = JDTCompiler.parseModernStream(
                createOutputForLines("1. ERROR in foo bar (at line 3) ", "2. ERROR in test (at line 5)"));
        assertEquals(2, messages.size());
        CompilerMessage message = messages.get(0);
        assertTrue(message.isError());
        assertEquals("", message.getMessage());
        assertEquals("foo bar", message.getFile());
        assertEquals(3, message.getStartLine());
        message = messages.get(1);
        assertTrue(message.isError());
        assertEquals("", message.getMessage());
        assertEquals("test", message.getFile());
        assertEquals(5, message.getStartLine());
    }

    @Test
    public void testParseModernStreamContextLines() throws IOException {
        List<CompilerMessage> messages = JDTCompiler.parseModernStream(createOutputForLines(
                "prologue (must be discarded)", "1. ERROR in foo bar (at line 3) ", "message line1", "message line2"));
        assertEquals(1, messages.size());
        CompilerMessage error = messages.get(0);
        assertEquals(EOL + "message line1" + EOL + "message line2", error.getMessage());
    }

    @Test
    public void testParseModernStreamIgnoreSeparatorLines() throws IOException {
        List<CompilerMessage> messages = JDTCompiler.parseModernStream(createOutputForLines(//
                "----------", //
                "1. ERROR in foo bar (at line 3) ", //
                "a context line", //
                "----------", //
                "", //
                "2. WARNING in test2 (at line 4)", //
                "second context line", //
                "----------"));
        assertEquals(2, messages.size());
        CompilerMessage error = messages.get(0);
        assertTrue(error.isError());
        assertEquals("foo bar", error.getFile());
        assertEquals(3, error.getStartLine());
        assertEquals(EOL + "a context line", error.getMessage());
        error = messages.get(1);
        assertFalse(error.isError());
        assertEquals("test2", error.getFile());
        assertEquals(4, error.getStartLine());
        assertEquals(EOL + "second context line", error.getMessage());
    }

    @Test
    public void testParseModernStreamErrorWithoutLine() throws IOException {
        List<CompilerMessage> messages = JDTCompiler.parseModernStream(createOutputForLines("1. ERROR in baz"));
        assertEquals(1, messages.size());
        CompilerMessage message = messages.get(0);
        assertTrue(message.isError());
        assertEquals("baz", message.getFile());
        assertEquals(-1, message.getStartLine());
    }

    @Test
    public void testParseModernStreamWarning() throws IOException {
        List<CompilerMessage> messages = JDTCompiler
                .parseModernStream(createOutputForLines("2. WARNING in foo (at line 4)"));
        assertEquals(1, messages.size());
        CompilerMessage message = messages.get(0);
        assertEquals(Kind.WARNING, message.getKind());
        assertEquals("foo", message.getFile());
        assertEquals(4, message.getStartLine());
    }

    @Test
    public void testParseModernStreamWarningNoNumber() throws IOException {
        List<CompilerMessage> messages = JDTCompiler
                .parseModernStream(createOutputForLines("WARNING in foo (at line 4)"));
        assertEquals(1, messages.size());
        CompilerMessage message = messages.get(0);
        assertFalse(message.isError());
        assertEquals("foo", message.getFile());
        assertEquals(4, message.getStartLine());
    }

    private static BufferedReader createOutputForLines(String... lines) {
        StringBuilder buf = new StringBuilder();
        for (String line : lines) {
            buf.append(line);
            buf.append("\n");
        }
        return new BufferedReader(new StringReader(buf.toString()));
    }
}
