/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.test.matcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Assert;

public class ComparingOutputStream extends OutputStream {

    private final File referenceFile;
    private FileInputStream reference;

    /**
     * Creates an <code>OutputStream</code> that verifies that exactly the content of the reference
     * file is written to it. Otherwise it throws throws an {@link AssertionError}.
     */
    public ComparingOutputStream(File referenceFile) {
        this.referenceFile = referenceFile;
        try {
            this.reference = new FileInputStream(referenceFile);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File with expected content not found: " + referenceFile);
        }
    }

    @Override
    public void write(int actual) throws IOException {
        int expected = reference.read();
        if (expected < 0 || ((byte) expected) != actual) {
            Assert.fail(failureMessage());
        }
    }

    @Override
    public void close() throws IOException {
        if (reference.read() != -1)
            Assert.fail(failureMessage());
        reference.close();
    }

    private String failureMessage() {
        return "Actual content does not match content of \"" + referenceFile + "\"";
    }

}
