/*******************************************************************************
 * Copyright (c) 2015 VDS Rail and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Enrico De Fent - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.docbundle;

import org.junit.Assert;

class AssertOnBuffer {
    final String[] lines;
    int curLine;
    final String buffer;

    public AssertOnBuffer(String buffer) {
        this.buffer = buffer;

        String lineSeparator = System.getProperty("line.separator");
        this.lines = buffer.split(lineSeparator);
    }

    public void assertMoreLines() {
        if (curLine == lines.length) {
            Assert.fail("expected more lines, found no more");
        }
    }

    public void assertNextLine(String line) {
        assertMoreLines();
        Assert.assertEquals("at line " + curLine, line, lines[curLine]);
        curLine++;
    }

    public void assertNoMoreLines() {
        if (curLine < lines.length) {
            Assert.fail(String.format("expected no more lines, found %d more", lines.length - curLine));
        }
    }
}
