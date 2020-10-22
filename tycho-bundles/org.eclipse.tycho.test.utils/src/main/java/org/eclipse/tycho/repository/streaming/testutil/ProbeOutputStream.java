/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP SE and others.
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
package org.eclipse.tycho.repository.streaming.testutil;

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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.provisional.p2.repository.IStateful;

/**
 * An output stream aggregating a summary of the zip entries.
 */
public class ProbeOutputStream extends OutputStream implements IStateful {

    static String MD5_SUM_ZEROS = "00000000000000000000000000000000";

    private ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
    private boolean byteBufferIsClosed = false;

    private IStatus externallySetStatus = Status.OK_STATUS;

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
        HashSet<String> result = new HashSet<>();

        try (ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(byteBuffer.toByteArray()))) {
            for (ZipEntry entry = zipStream.getNextEntry(); entry != null; entry = zipStream.getNextEntry()) {
                result.add(entry.getName());
            }
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

    @Override
    public void setStatus(IStatus status) {
        externallySetStatus = status;
    }

    @Override
    public IStatus getStatus() {
        return externallySetStatus;
    }

}
