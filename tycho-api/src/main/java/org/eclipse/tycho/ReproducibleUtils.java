/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.Collectors;

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
     *            the properties object to write to the file.
     * @param file
     *            the file to write to. All the missing parent directories are also created.
     * @throws IOException
     *             if writing the property list to the specified output stream throws an
     *             IOException.
     */
    public static void storeProperties(Properties properties, Path file) throws IOException {
        final Path folder = file.getParent();
        if (folder != null) {
            Files.createDirectories(folder);
        }
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        properties.store(baos, null);
        final String content = baos.toString(StandardCharsets.ISO_8859_1).lines().filter(line -> !line.startsWith("#"))
                .sorted().collect(Collectors.joining("\n", "", "\n"));
        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(file))) {
            os.write(content.getBytes(StandardCharsets.ISO_8859_1));
        }
    }
}
