/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.test.util;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.eclipse.tycho.test.util.NoopOutputStream;

/**
 * An output stream computing the MD5 of the received data.
 */
public class Md5DigestOutputStream extends OutputStream {

    private DigestOutputStream digestStream;

    public Md5DigestOutputStream() {
        try {
            digestStream = new DigestOutputStream(new NoopOutputStream(), MessageDigest.getInstance("MD5"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String md5AsHex() {
        byte[] bytes = digestStream.getMessageDigest().digest();
        return new BigInteger(1, bytes).toString(16);
    }

    @Override
    public void write(byte[] b) throws IOException {
        digestStream.write(b);
    }

    @Override
    public void write(int b) throws IOException {
        digestStream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        digestStream.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        digestStream.flush();
    }

    @Override
    public void close() throws IOException {
        digestStream.close();
    }

}
