/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.test.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * An output stream aggregating a summary of the zip entries.
 */
public class ProbeOutputStream extends OutputStream {

    static String MD5_SUM_ZEROS = "00000000000000000000000000000000";

    private ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
    private boolean byteBufferIsClosed = false;

    public int writtenBytes() {
        return byteBuffer.size();
    }

    public boolean isClosed() {
        return byteBufferIsClosed;
    }

    /**
     * @return the entries obtained from reading the written data as {@link ZipInputStream}; empty
     *         if written data was not a zip file.
     */
    public Set<String> getFilesInZip() throws IOException {
        HashSet<String> result = new HashSet<String>();

        ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(byteBuffer.toByteArray()));
        try {
            for (ZipEntry entry = zipStream.getNextEntry(); entry != null; entry = zipStream.getNextEntry()) {
                result.add(entry.getName());
            }
        } finally {
            zipStream.close();
        }

        return result;
    }

    public String md5AsHex() throws IOException {
        final DigestOutputStream digestStream = createMd5DigestStream();
        writeBufferedContent(digestStream);
        return formatDigestAsPaddedHex(digestStream, MD5_SUM_ZEROS);
    }

    private DigestOutputStream createMd5DigestStream() {
        try {
            return new DigestOutputStream(new NoopOutputStream(), MessageDigest.getInstance("MD5"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeBufferedContent(final DigestOutputStream digestStream) throws IOException {
        byte[] content = byteBuffer.toByteArray();
        digestStream.write(content, 0, content.length);
    }

    private String formatDigestAsPaddedHex(final DigestOutputStream digestStream, String padding) {
        byte[] digest = digestStream.getMessageDigest().digest();
        String digestHex = new BigInteger(1, digest).toString(16);
        String paddedDigestHex = padding.substring(0, padding.length() - digestHex.length()) + digestHex;
        return paddedDigestHex;
    }

    @Override
    public void write(byte[] b) throws IOException {
        byteBuffer.write(b);
    }

    @Override
    public void write(int b) throws IOException {
        byteBuffer.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        byteBuffer.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        byteBuffer.flush();
    }

    @Override
    public void close() throws IOException {
        byteBufferIsClosed = true;
        byteBuffer.close();
    }

}
