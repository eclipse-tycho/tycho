/*******************************************************************************
 * Copyright (c) 2015, 2020 VDS Rail and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Enrico De Fent - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.docbundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class AssertOnBuffer {
    final String[] lines;
    int curLine;
    final String buffer;

    public AssertOnBuffer(String buffer) {
        this.buffer = buffer;

        String lineSeparator = System.lineSeparator();
        this.lines = buffer.split(lineSeparator);
    }

    public void assertMoreLines() {
        if (curLine == lines.length) {
            fail("expected more lines, found no more");
        }
    }

    public void assertNextLine(String line) {
        assertMoreLines();
        assertEquals(line, lines[curLine], "at line " + curLine);
        curLine++;
    }

    public void assertNoMoreLines() {
        if (curLine < lines.length) {
            fail(String.format("expected no more lines, found %d more", lines.length - curLine));
        }
    }
}
