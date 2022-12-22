/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.artifactcomparator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;

/**
 * Input stream to carry some important information for comparison and allows direct access to the
 * underlying buffer.
 * 
 * @author christoph
 *
 */
public class ComparatorInputStream extends ByteArrayInputStream {

    private final byte[] content;
    private final String name;

    public ComparatorInputStream(InputStream stream, String name) throws IOException {
        this(IOUtils.toByteArray(stream), name);
    }

    public ComparatorInputStream(byte[] content, String name) {
        super(content);
        this.content = content;
        this.name = name;
    }

    /**
     * 
     * @return the total number of bytes of this stream
     */
    public int size() {
        return content.length;
    }

    /**
     * @return a new stream backed by this stream data that could be read independently
     */
    public InputStream asNewStream() {
        return new ByteArrayInputStream(content);
    }

    /**
     * @param charset
     * @return the content of this stream as a string with the given charset
     */

    public String asString(Charset charset) {
        return new String(content, charset);
    }

    public byte[] asBytes() {
        return content.clone();
    }

    /**
     * Compares this stream directly to another stream on a by-by-byte basis, this is independent of
     * the state of the streams, e.g number of bytes read, closing or mark state.
     * 
     * @param other
     * @return {@link ArtifactDelta#DEFAULT} if there is any difference or <code>null</code>
     *         otherwise.
     */
    public ArtifactDelta compare(ComparatorInputStream other) {
        if (Arrays.equals(content, other.content)) {
            return ArtifactDelta.NO_DIFFERENCE;
        }
        return ArtifactDelta.DEFAULT;
    }

}
