/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility methods for reproducible builds.
 */
public class ReproducibleUtils {
    private ReproducibleUtils() {
    }

    /**
     * Writes the property list to the output stream in a reproducible way. The java.util.Properties
     * class writes the lines in a non-reproducible order, adds a non-reproducible timestamp and
     * uses platform-dependent new line characters.
     * 
     * @param properties
     *            the properties object to write to the output stream.
     * @param out
     *            an output stream.
     * @param comments
     *            a description of the property list.
     * @throws IOException
     *             if writing the property list to the specified output stream throws an
     *             IOException.
     */
    public static void storeProperties(Properties properties, OutputStream out, String comments) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        properties.store(baos, comments);
        final String originalContent = baos.toString(StandardCharsets.ISO_8859_1);
        // Keep the comment lines if any
        final long commentLinesNb = (comments != null) ? comments.lines().count() : 0;
        final Stream<String> commentLinesStream = originalContent.lines().limit(commentLinesNb);
        // Drop the timestamp comment, order the lines and use a system-independent new line
        final String contentFixed = Stream
                .concat(commentLinesStream, originalContent.lines().skip(commentLinesNb + 1).sorted())
                .collect(Collectors.joining("\n", "", "\n"));
        out.write(contentFixed.getBytes(StandardCharsets.ISO_8859_1));
    }
}
