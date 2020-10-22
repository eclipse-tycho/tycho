/*******************************************************************************
 * Copyright (c) 2014, 2019 SAP SE and others.
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
package org.eclipse.tycho.test.util;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.codehaus.plexus.util.IOUtil;

public class ArchiveContentUtil {

    /**
     * Returns a list of file and folder entries in the given zip archive.
     */
    public static Set<String> getFilesInZip(File archive) throws Exception {
        final HashSet<String> result = new HashSet<>();

        visitEntries(archive, (ZipEntryVisitor) (entry, stream) -> {
            result.add(entry.getName());
            return true;
        });

        return result;
    }

    /**
     * Returns the content of the given file as string.
     */
    public static String getFileContent(File archive, final String fileInArchive) throws Exception {
        final String[] result = new String[1];

        visitEntries(archive, (ZipEntryVisitor) (entry, stream) -> {
            if (fileInArchive.equals(entry.getName())) {
                result[0] = IOUtil.toString(stream);
                return false;
            }
            return true;
        });

        if (result[0] == null) {
            throw new IllegalArgumentException("File not found in archive: " + fileInArchive);
        }
        return result[0];
    }

    static void visitEntries(File archive, ZipEntryVisitor visitor) throws Exception {
        try (FileInputStream fileStream = new FileInputStream(archive)) {
            ZipInputStream zipStream = new ZipInputStream(fileStream);
            try {
                visitEntries(zipStream, visitor);
            } finally {
                zipStream.close();
            }
        }

    }

    private static void visitEntries(ZipInputStream zipStream, ZipEntryVisitor visitor) throws Exception {
        for (ZipEntry entry = zipStream.getNextEntry(); entry != null; entry = zipStream.getNextEntry()) {
            boolean continueVisiting = visitor.visitEntry(entry, zipStream);

            if (!continueVisiting) {
                return;
            }
        }
    }

    private interface ZipEntryVisitor {
        boolean visitEntry(ZipEntry entry, ZipInputStream stream) throws Exception;
    }
}
